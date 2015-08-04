package com.uncharted.mosaic.generation

import com.uncharted.mosaic.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosaic.generation.projection.Projection
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.{Accumulable, SparkContext}
import org.apache.spark.sql.Row

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Object pool for TileGenerationAccumulableParams
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @param bExtractor a (broadcasted) mechanism for grabbing the "value" column from a source record
 * @param bBinAggregator the (broadcasted) desired bin analytic strategy
 * @param bTileAggregator the (broadcasted) desired tile analytic strategy
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class TileGenerationAccumulableParamPool[T, U: ClassTag](
  sc: SparkContext,
  bProjection: Broadcast[Projection],
  bExtractor: Broadcast[ValueExtractor[T]],
  bBinAggregator: Broadcast[Aggregator[T, U, _]]
) {
  private val pool = mutable.Stack[Accumulable[Array[U], ((Int, Int), Row)]]()

  //will store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }

  def reserve(): Accumulable[Array[U], ((Int, Int), Row)] = {
    pool.synchronized({
      if (pool.length > 0) {
        pool.pop()
      } else {
        val bins = makeBins(bProjection.value.xBins*bProjection.value.yBins, bBinAggregator.value.default)
        val param = new TileGenerationAccumulableParam[T,U](bProjection, bExtractor, bBinAggregator)
        sc.accumulable(bins)(param)
      }
    })
  }

  def release(accumulator: Accumulable[Array[U], ((Int, Int), Row)]): Unit = {
    pool.synchronized({
      pool.push(accumulator)
    })
  }

  def releaseAll(): Unit = {
    pool.clear
  }
}
