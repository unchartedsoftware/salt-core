package com.unchartedsoftware.mosaic.core.generation.accumulator

import com.unchartedsoftware.mosaic.core.analytic.{Aggregator, ValueExtractor}
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.generation.BatchTileGenerator
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Row, DataFrame}
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Tile Generator which utilizes a map/reduce approach to
 * generating large tile sets.
 * @param projection the  projection from data to some space (i.e. 2D or 1D)
 * @param extractor a mechanism for grabbing the "value" column from a source record
 * @param binAggregator the desired bin analytic strategy
 * @param tileAggregator the desired tile analytic strategy
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class MapReduceTileGenerator[TC: ClassTag, T, U: ClassTag, V, W, X](
  sc: SparkContext,
  projection: Projection[TC],
  extractor: ValueExtractor[T],
  binAggregator: Aggregator[T, U, V],
  tileAggregator: Aggregator[V, W, X])(implicit tileCoordManifest: Manifest[TC])
extends BatchTileGenerator[TC, T, U, V, W, X](sc, projection, extractor, binAggregator, tileAggregator) {

  /**
   * Converts raw input data into an RDD which contains only what we need:
   * rows which contain a tile coordinate, a bin and a value.
   */
  private def transformData(
    dataFrame: DataFrame,
    bProjection: Broadcast[Projection[TC]],
    bExtractor: Broadcast[ValueExtractor[T]],
    bLevels: Broadcast[Seq[Int]]): RDD[(TC, (Int, Option[T]))] = {
    dataFrame.flatMap(r => {

      //extract value for this row
      val value = bExtractor.value.rowToValue(r)

      //map all input data to an RDD of (TC, (bin, Option[value]))
      bLevels.value
      .flatMap(l => {
        val coords = bProjection.value.rowToCoords(r, l)
        if (coords.isDefined) {
          Seq((coords.get._1, (coords.get._2, value)))
        } else {
          Seq()
        }
      })
    })
  }

  //will store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }

  def generate(dataFrame: DataFrame, levels: Seq[Int]): RDD[TileData[TC, V, X]] = {
    dataFrame.cache //ensure data is cached

    //broadcast stuff we'll use on the workers throughout our tilegen process
    val bProjection = sc.broadcast(projection)
    val bExtractor = sc.broadcast(extractor)
    val bBinAggregator = sc.broadcast(binAggregator)
    val bTileAggregator = sc.broadcast(tileAggregator)
    val bLevels = sc.broadcast(levels)

    //Start by transforming the raw input data into a distributed RDD(TC, (bin, Option[value]))
    val transformedData = transformData(dataFrame, bProjection, bExtractor, bLevels)

    //Now we are going to use combineByKey, but we have to make some lambdas first:
      //create a new combiner, with a fresh set of bins
    val createCombiner = (firstValue: (Int, Option[T])) => {
      val binAggregator = bBinAggregator.value
      val bins: Array[U] = makeBins(bProjection.value.bins, binAggregator.default)
      bins(firstValue._1) = binAggregator.add(binAggregator.default, firstValue._2)
      bins
    }: Array[U]
      //how to add a value to a combiner
    val mergeValue = (combiner: Array[U], newValue: (Int, Option[T])) => {
      val binAggregator = bBinAggregator.value
      combiner(newValue._1) = binAggregator.add(combiner(newValue._1), newValue._2)
      combiner
    }: Array[U]
      //how to merge combiners
    val mergeCombiners = (r1: Array[U], r2: Array[U]) => {
      val limit = bProjection.value.bins
      val binAggregator = bBinAggregator.value
      for (i <- 0 until limit) {
        r1(i) = binAggregator.merge(r1(i), r2(i))
      }
      r1
    }: Array[U]

    //Do the actual combineByKey to produce finished bin data
    val tileData = transformedData.combineByKey(createCombiner, mergeValue, mergeCombiners)

    val result = tileData.map(t => {
      val binAggregator = bBinAggregator.value
      val tileAggregator = bTileAggregator.value
      val projection = bProjection.value
      var tile: W = tileAggregator.default
      val key = t._1
      var binsTouched = 0

      val finishedBins = t._2.map(a => {
        if (!a.equals(binAggregator.default)) binsTouched+=1
        val bin = binAggregator.finish(a)
        tile = tileAggregator.add(tile, Some(bin))
        bin
      })
      new TileData[TC, V, X](key, finishedBins, binsTouched, binAggregator.finish(binAggregator.default), tileAggregator.finish(tile), projection)
    })

    bLevels.unpersist
    bProjection.unpersist
    bExtractor.unpersist
    bBinAggregator.unpersist
    bTileAggregator.unpersist

    result
  }
}
