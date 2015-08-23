package com.unchartedsoftware.mosaic.core.generation.accumulator

import com.unchartedsoftware.mosaic.core.analytic.{Aggregator, ValueExtractor}
import com.unchartedsoftware.mosaic.core.projection.{TileCoord, Projection}
import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.generation.TileGenerator
import org.apache.spark.{Accumulable, SparkContext}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Row, DataFrame}
import scala.reflect.ClassTag
import scala.util.Try
import scala.collection.mutable.{HashMap, ListBuffer}

/**
 * Tile Generator for a batch of tiles, using accumulators
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
class AccumulatorTileGenerator[TC <:TileCoord : ClassTag, T, U: ClassTag, V, W, X](
  sc: SparkContext,
  projection: Projection[TC],
  extractor: ValueExtractor[T],
  binAggregator: Aggregator[T, U, V],
  tileAggregator: Aggregator[V, W, X])(implicit tileCoordManifest: Manifest[TC])
extends TileGenerator[TC, T, U, V, W, X](sc, projection, extractor, binAggregator, tileAggregator) {

  private val pool = new TileAccumulablePool[TC, T, U, V](sc)

  //TODO find a way to eliminate inCoords by using reflection to locate a zero-arg constructor and invoke it.
  //then we can remove this from the superclass as well
  def generate(dataFrame: DataFrame, tiles: Seq[TC]): HashMap[TC, TileData[TC, V, X]] = {
    dataFrame.cache //ensure data is cached

    //broadcast stuff we'll use on the workers throughout our tilegen process
    val bProjection = sc.broadcast(projection)
    val bExtractor = sc.broadcast(extractor)
    val bBinAggregator = sc.broadcast(binAggregator)
    val bTileAggregator = sc.broadcast(tileAggregator)

    //build a map of our tile accumulators using the pool
    val accumulators = new HashMap[TC, Accumulable[Array[U], (Int, Row)]]()
    val toRelease = ListBuffer.empty[TileAccumulable[TC, T, U, V]]
    for (i <- 0 until tiles.length) {
      val acc = pool.reserve(bProjection, bExtractor, bBinAggregator)
      accumulators.put(tiles(i), acc.accumulable)
      toRelease.append(acc)
    }

    //deliberately broadcast this 'incorrectly', so we have one copy on each worker, even though they'll diverge
    val reusableCoord: TC = tileCoordManifest.runtimeClass.newInstance.asInstanceOf[TC]
    val bCoords = sc.broadcast(reusableCoord)

    //map requested tile set to a map of level -> tiles_at_level
    val levelMappedTiles = tiles.groupBy(c => c.z)

    val result = _sanitizedClosureGenerate(bProjection, bExtractor, bBinAggregator, bTileAggregator, bCoords, dataFrame, levelMappedTiles, accumulators)

    //release accumulators back to pool, and unpersist broadcast variables
    toRelease.foreach(a => {
      pool.release(a)
    })
    bProjection.unpersist
    bExtractor.unpersist
    bBinAggregator.unpersist
    bTileAggregator.unpersist
    bCoords.unpersist

    result
  }

  /**
   * Since spark serializes closures, everything within the closure must be serializable
   * This does the real work of generate, excluding the handling of anything that isn't
   * serializable and needs to be dealt with on the master.
   */
  private def _sanitizedClosureGenerate(
    bProjection: Broadcast[Projection[TC]],
    bExtractor: Broadcast[ValueExtractor[T]],
    bBinAggregator: Broadcast[Aggregator[T, U, V]],
    bTileAggregator: Broadcast[Aggregator[V, W, X]],
    bCoords: Broadcast[TC],
    dataFrame: DataFrame,
    levelMappedTiles: Map[Int, Seq[TC]],
    accumulators: HashMap[TC, Accumulable[Array[U], (Int, Row)]]
  ): HashMap[TC, TileData[TC, V, X]] = {

    //generate bin data by iterating over each row of the source data frame
    dataFrame.foreach(row => {
      Try({
        val _coord = bCoords.value
        levelMappedTiles.foreach(l => {
          val bin = bProjection.value.rowToCoords(row, l._1, _coord)
          //bin is defined when we are in the bounds of the projection
          if (bin.isDefined) {
            if (accumulators.contains(_coord)) {
              accumulators.get(_coord).get.add((bin.get, row))
            }
          }
        })
      })
    })

    //finish tile by computing tile-level statistics
    //TODO parallelize on workers (if the number of tiles is heuristically large) to avoid memory overloading on the master?
    accumulators map { case (key, accumulator) => {
      val binAggregator = bBinAggregator.value
      val tileAggregator = bTileAggregator.value
      val projection = bProjection.value
      var tile: W = tileAggregator.default
      var binsTouched = 0

      val finishedBins = accumulator.value.map(a => {
        if (!a.equals(binAggregator.default)) binsTouched+=1
        val bin = binAggregator.finish(a)
        tile = tileAggregator.add(tile, Some(bin))
        bin
      })
      val info = new TileData[TC, V, X](key, finishedBins, binsTouched, binAggregator.finish(binAggregator.default), tileAggregator.finish(tile), projection)
      (key, info)
    }}
  }
}
