package com.uncharted.mosaic.generation

import com.uncharted.mosaic.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosaic.generation.projection.Projection
import org.apache.spark.{Accumulable, SparkContext}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Row, DataFrame}
import scala.reflect.ClassTag

import scala.collection.mutable.HashMap

/**
 * Tile Generator for a batch of tiles
 * @param projection the  projection from data to some space (i.e. 2D or 1D)
 * @param extractor a mechanism for grabbing the "value" column from a source record
 * @param binAggregator the desired bin analytic strategy
 * @param tileAggregator the desired tile analytic strategy
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class TileGenerator[T,U: ClassTag,V,W,X](
  projection: Projection,
  extractor: ValueExtractor[T],
  binAggregator: Aggregator[T, U, V],
  tileAggregator: Aggregator[V, W, X]) {

  //builds an array that can store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }

  def generate(sc: SparkContext, dataFrame: DataFrame, tiles: Seq[(Int, Int, Int)]): HashMap[(Int, Int, Int), TileData[V, X]] = {
    val bProjection = sc.broadcast(projection)
    val bExtractor = sc.broadcast(extractor)
    val bBinAggregator = sc.broadcast(binAggregator)

    val accumulators = new HashMap[(Int, Int, Int), Accumulable[Array[U], ((Int, Int), Row)]]()
    for (i <- 0 until tiles.length) {
      val bins = makeBins(projection.xBins*projection.yBins, binAggregator.default)
      val param = new TileGenerationAccumulableParam[T,U,V](bProjection, bExtractor, bBinAggregator)
      accumulators.put(tiles(i), sc.accumulable(bins)(param))
    }

    //generate data by iterating over each row of the source data frame
    dataFrame
      .foreach(row => {
      val _coords = new Array[(Int, Int, Int, Int, Int)](bProjection.value.maxZoom + 1)
      val inBounds = bProjection.value.rowToCoords(row, _coords)
      if (inBounds) {
        _coords.foreach((c: (Int, Int, Int, Int, Int)) => {
          val coord = (c._1, c._2, c._3)
          if (accumulators.contains(coord)) {
            accumulators.get(coord).get.add(((c._4, c._5), row))
          }
        })
      }
    })
    val result = accumulators map { case (key, value) => {
      var tile: W = tileAggregator.default
      var binsTouched = 0
      //this needs to copy the results from the accumulator, not reference them...
      val finishedBins = value.value.map(a => {
        if (!a.equals(binAggregator.default)) binsTouched+=1
        val bin = binAggregator.finish(a)
        tile = tileAggregator.add(tile, Some(bin))
        bin
      })
      val info = new TileData[V, X](key, finishedBins, binsTouched, binAggregator.finish(binAggregator.default), tileAggregator.finish(tile), projection)
      (key, info)
    }}

    bProjection.unpersist
    bExtractor.unpersist
    bBinAggregator.unpersist

    result
  }
}
