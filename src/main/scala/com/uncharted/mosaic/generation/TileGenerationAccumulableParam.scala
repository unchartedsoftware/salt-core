package com.uncharted.mosaic.generation

import com.uncharted.mosaic.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosaic.generation.projection.Projection
import scala.reflect.ClassTag
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.{AccumulableParam, Accumulable}
import org.apache.spark.sql.Row
import scala.util.Try

/**
 * Accumulator which aggregates bin values for a tile
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @param bExtractor a (broadcasted) mechanism for grabbing the "value" column from a source record
 * @param bBinAggregator the (broadcasted) desired bin analytic strategy
 * @tparam T Input data type for aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 */
class TileGenerationAccumulableParam[T, U: ClassTag, V](
    private var bProjection: Broadcast[Projection],
    private var bExtractor: Broadcast[ValueExtractor[T]],
    private var bBinAggregator: Broadcast[Aggregator[T, U, V]]
  ) extends AccumulableParam[Array[U], ((Int, Int), Row)]() {

  def reset(
    newBProjection: Broadcast[Projection],
    newBExtractor: Broadcast[ValueExtractor[T]],
    newBBinAggregator: Broadcast[Aggregator[T, U, V]]
  ): Unit = {
    bProjection = newBProjection
    bExtractor = newBExtractor
    bBinAggregator = newBBinAggregator
  }

  //will store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }

  override def addAccumulator(r: Array[U], t: ((Int, Int), Row)): Array[U] = {
    val bin = t._1
    val row = t._2
    val index = bin._1 + bin._2*bProjection.value.xBins
    val current = r(index)
    Try({
      val value: Option[T] = bExtractor.value.rowToValue(row)
      r(index) = bBinAggregator.value.add(current, value)
    })
    r
  }

  override def addInPlace(r1: Array[U], r2: Array[U]): Array[U] = {
    val limit = bProjection.value.xBins*bProjection.value.yBins
    val binAggregator = bBinAggregator.value
    for (i <- 0 until limit) {
      r1(i) = binAggregator.merge(r1(i), r2(i))
    }
    r1
  }

  override def zero(initialValue: Array[U]): Array[U] = {
    makeBins(bProjection.value.xBins*bProjection.value.yBins, bBinAggregator.value.default)
  }
}
