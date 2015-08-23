package com.unchartedsoftware.mosaic.core.generation.accumulator

import com.unchartedsoftware.mosaic.core.analytic.{Aggregator, ValueExtractor}
import com.unchartedsoftware.mosaic.core.projection.Projection
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.{Accumulable, SparkContext}
import org.apache.spark.sql.Row

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * A wrapper which links Accumulables to their associated AccumulableParam
 */
class TileAccumulable[TC, T, U: ClassTag, V](
  val accumulable: Accumulable[Array[U], (Int, Row)],
  val param: TileGenerationAccumulableParam[TC, T, U, V]
) {}

/**
 * Object pool for TileGenerationAccumulableParams
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @param bExtractor a (broadcasted) mechanism for grabbing the "value" column from a source record
 * @param bBinAggregator the (broadcasted) desired bin analytic strategy
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 */
class TileAccumulablePool[TC, T, U: ClassTag, V](sc: SparkContext) {
  private val pool = mutable.Stack[TileAccumulable[TC, T, U, V]]()

  //will store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }

  def reserve(
    bProjection: Broadcast[Projection[TC]],
    bExtractor: Broadcast[ValueExtractor[T]],
    bBinAggregator: Broadcast[Aggregator[T, U, V]]
  ): TileAccumulable[TC, T, U, V] = {
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
        val bins = makeBins(bProjection.value.bins, bBinAggregator.value.default)
        val param = new TileGenerationAccumulableParam[TC, T, U, V](bProjection, bExtractor, bBinAggregator)
        new TileAccumulable(sc.accumulable(bins)(param), param)
      }
    })
  }

  def release(accumulator: TileAccumulable[TC, T, U, V]): Unit = {
    pool.synchronized({
      pool.push(accumulator)
    })
  }

  def clear(): Unit = {
    pool.clear
  }
}
