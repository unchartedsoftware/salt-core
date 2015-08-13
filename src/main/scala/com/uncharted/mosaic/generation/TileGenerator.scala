package com.uncharted.mosaic.generation

import com.uncharted.mosaic.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosaic.generation.projection.Projection
import org.apache.spark.{Accumulable, SparkContext}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Row, DataFrame}
import scala.reflect.ClassTag
import scala.util.Try
import scala.collection.mutable.{HashMap, ListBuffer}

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
  sc: SparkContext,
  projection: Projection,
  extractor: ValueExtractor[T],
  binAggregator: Aggregator[T, U, V],
  tileAggregator: Aggregator[V, W, X]) {

  private val pool = new TileAccumulablePool[T, U, V](sc)

  def generate(dataFrame: DataFrame, tiles: Seq[(Int, Int, Int)]): HashMap[(Int, Int, Int), TileData[V, X]] = {
    val bProjection = sc.broadcast(projection)
    val bExtractor = sc.broadcast(extractor)
    val bBinAggregator = sc.broadcast(binAggregator)
    val bTileAggregator = sc.broadcast(tileAggregator)

    val accumulators = new HashMap[(Int, Int, Int), Accumulable[Array[U], ((Int, Int), Row)]]()
    val toRelease = ListBuffer.empty[TileAccumulable[T, U, V]]
    for (i <- 0 until tiles.length) {
      val acc = pool.reserve(bProjection, bExtractor, bBinAggregator)
      accumulators.put(tiles(i), acc.accumulable)
      toRelease.append(acc)
    }

    //deliberately broadcast this 'incorrectly', so we have one copy on each worker, even though they'll diverge
    val bCoords = sc.broadcast(new Array[(Int, Int, Int, Int, Int)](projection.maxZoom + 1))

    val result = _sanitizedClosureGenerate(bProjection, bExtractor, bBinAggregator, bTileAggregator, bCoords, dataFrame, tiles, accumulators)

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
    bProjection: Broadcast[Projection],
    bExtractor: Broadcast[ValueExtractor[T]],
    bBinAggregator: Broadcast[Aggregator[T, U, V]],
    bTileAggregator: Broadcast[Aggregator[V, W, X]],
    bCoords: Broadcast[Array[(Int, Int, Int, Int, Int)]],
    dataFrame: DataFrame,
    tiles: Seq[(Int, Int, Int)],
    accumulators: HashMap[(Int, Int, Int), Accumulable[Array[U], ((Int, Int), Row)]]
  ): HashMap[(Int, Int, Int), TileData[V, X]] = {

    //generate bin data by iterating over each row of the source data frame
    dataFrame.foreach(row => {
      Try({
        val _coords = bCoords.value
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
      val info = new TileData[V, X](key, finishedBins, binsTouched, binAggregator.finish(binAggregator.default), tileAggregator.finish(tile), projection)
      (key, info)
    }}
  }
}
