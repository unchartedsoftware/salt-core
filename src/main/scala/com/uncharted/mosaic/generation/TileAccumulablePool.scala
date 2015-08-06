package com.uncharted.mosaic.generation

import com.uncharted.mosaic.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosaic.generation.projection.Projection
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.{Accumulable, SparkContext}
import org.apache.spark.sql.Row

import scala.collection.mutable
import scala.reflect.ClassTag

class TileAccumulable[T, U: ClassTag, V](val accumulable: Accumulable[Array[U], ((Int, Int), Row)], val param: TileGenerationAccumulableParam[T, U, V]) {}

/**
 * Object pool for TileGenerationAccumulableParams
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @param bExtractor a (broadcasted) mechanism for grabbing the "value" column from a source record
 * @param bBinAggregator the (broadcasted) desired bin analytic strategy
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 */
class TileAccumulablePool[T, U: ClassTag, V](sc: SparkContext) {
  private val pool = mutable.Stack[TileAccumulable[T, U, V]]()

  //will store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }

  def reserve(
    bProjection: Broadcast[Projection],
    bExtractor: Broadcast[ValueExtractor[T]],
    bBinAggregator: Broadcast[Aggregator[T, U, V]]
  ): TileAccumulable[T, U, V] = {
    pool.synchronized({
      if (pool.length > 0) {
        val acc = pool.pop()
        //clear existing data and place new broadcasted projection/extractor/aggregator into accumulator
        acc.param.reset(bProjection, bExtractor, bBinAggregator)
        val bins = acc.accumulable.value
        val default = bBinAggregator.value.default
        for (i <- 0 until bins.length) {
          bins(i) = default
        }
        acc.accumulable.setValue(bins)
        acc
      } else {
        val bins = makeBins(bProjection.value.xBins*bProjection.value.yBins, bBinAggregator.value.default)
        val param = new TileGenerationAccumulableParam[T,U,V](bProjection, bExtractor, bBinAggregator)
        new TileAccumulable(sc.accumulable(bins)(param), param)
      }
    })
  }

  def release(accumulator: TileAccumulable[T, U, V]): Unit = {
    pool.synchronized({
      pool.push(accumulator)
    })
  }

  def clear(): Unit = {
    pool.clear
  }
}
