package com.uncharted.mosiac.generation

import com.uncharted.mosiac.generation.analytic.{Analytic, ValueExtractor}
import com.uncharted.mosiac.generation.projection.Projection
import org.apache.spark.broadcast.Broadcast

import scala.collection.mutable

/**
 * Object pool for TileAggregators
 */
class TileAggregatorPool[T, U, V](
  bProjection: Broadcast[Projection],
  bExtractor: Broadcast[ValueExtractor[T]],
  bAnalytic: Broadcast[Analytic[T, U, V]]
) {
  private val pool = mutable.Stack[TileAggregator[T, U, V]]()

  def reserve(coords: (Int, Int, Int)): TileAggregator[T, U, V] = {
    pool.synchronized({
      if (pool.length > 0) {
        val result = pool.pop()
        result.reset(coords)
        result
      } else {
        val result = new TileAggregator(coords, bProjection, bExtractor, bAnalytic)
        result
      }
    })
  }

  def release(aggregator: TileAggregator[T, U, V]): Unit = {
    pool.synchronized({
      pool.push(aggregator)
    })
  }
}
