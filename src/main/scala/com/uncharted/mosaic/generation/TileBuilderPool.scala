package com.uncharted.mosiac.generation

import com.uncharted.mosiac.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosiac.generation.projection.Projection
import org.apache.spark.broadcast.Broadcast

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Object pool for TileAggregators
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @param bExtractor a (broadcasted) mechanism for grabbing the "value" column from a source record
 * @param bBinAggregator the (broadcasted) desired bin analytic strategy
 * @param bTileAggregator the (broadcasted) desired tile analytic strategy
 * @tparam T Input data type for aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class TileBuilderPool[T, U: ClassTag, V, W, X](
  bProjection: Broadcast[Projection],
  bExtractor: Broadcast[ValueExtractor[T]],
  bBinAggregator: Broadcast[Aggregator[T, U, V]],
  bTileAggregator: Broadcast[Aggregator[V, W, X]]
) {
  private val pool = mutable.Stack[TileBuilder[T, U, V, W, X]]()

  def reserve(coords: (Int, Int, Int)): TileBuilder[T, U, V, W, X] = {
    pool.synchronized({
      if (pool.length > 0) {
        val result = pool.pop()
        result.reset(coords)
        result
      } else {
        val result = new TileBuilder[T,U,V,W,X](coords, bProjection, bExtractor, bBinAggregator, bTileAggregator)
        result
      }
    })
  }

  def release(aggregator: TileBuilder[T, U, V, W, X]): Unit = {
    pool.synchronized({
      pool.push(aggregator)
    })
  }
}
