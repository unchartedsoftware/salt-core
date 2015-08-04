package com.uncharted.mosiac.generation

import com.uncharted.mosiac.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosiac.generation.projection.Projection
import org.apache.spark.{Accumulable, SparkContext}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Row, DataFrame}

import scala.collection.mutable.HashMap

/**
 * Tile Generator for a batch of tiles
 * @param aggregatorPool A TileAggregatorPool for this generator
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @tparam T Input data type for aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class TileGenerator[T,U,V,W,X](
  aggregatorPool: TileBuilderPool[T,U,V,W,X],
  projection: Projection) {

  def generate(sc: SparkContext, dataFrame: DataFrame, tiles: Seq[(Int, Int, Int)]): HashMap[(Int, Int, Int), TileBuilder[T,U,V,W,X]] = {
    val accumulators = new HashMap[(Int, Int, Int), Accumulable[TileBuilder[T,U,V,W,X], ((Int, Int), Row)]]()
    for (i <- 0 until tiles.length) {
      val coord = tiles(i)
      val param = new TileGenerationAccumulableParam[T,U,V,W,X]()
      val accumulator = sc.accumulable(aggregatorPool.reserve(coord))(param)
      accumulators.put(coord, accumulator)
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

    accumulators map { case (key, value) => (key, value.value)}
  }
}
