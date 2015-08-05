package com.uncharted.mosaic.generation

import com.uncharted.mosaic.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosaic.generation.projection.Projection
import org.apache.spark.{Accumulable, SparkContext}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Row, DataFrame}

import scala.collection.mutable.HashMap

/**
 * Tile Generator for a batch of tiles
 * @param accumulatorPool A pool of tile bin value accumulators for this generator
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class TileGenerator[T,U,V,W,X](
  accumulatorPool: TileGenerationAccumulableParamPool[T,U,V],
  projection: Projection,
  binAggregator: Aggregator[T, U, V],
  tileAggregator: Aggregator[V, W, X]) {

  def generate(sc: SparkContext, dataFrame: DataFrame, tiles: Seq[(Int, Int, Int)]): HashMap[(Int, Int, Int), TileData[V, X]] = {
    val accumulators = new HashMap[(Int, Int, Int), Accumulable[Array[U], ((Int, Int), Row)]]()
    for (i <- 0 until tiles.length) {
      accumulators.put(tiles(i), accumulatorPool.reserve)
    }

    //deliberately broadcast this 'incorrectly', so we have one copy on each worker, even though they'll diverge
    val _bCoords = sc.broadcast(new Array[(Int, Int, Int, Int, Int)](projection.maxZoom + 1))

    val bProjection = sc.broadcast(projection)
    //generate data by iterating over each row of the source data frame
    dataFrame
      .foreach(row => {
      val _coords = _bCoords.value
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

    _bCoords.unpersist
    bProjection.unpersist

    accumulators map { case (key, value) => {
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
      accumulatorPool.release(value)
      (key, info)
    }}
  }
}
