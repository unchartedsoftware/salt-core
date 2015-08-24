package com.unchartedsoftware.mosaic.core.generation.accumulator

import com.unchartedsoftware.mosaic.core.analytic.{Aggregator, ValueExtractor}
import com.unchartedsoftware.mosaic.core.projection.Projection
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
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam T Input data type for aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 */
class TileGenerationAccumulableParam[TC, T, U: ClassTag, V](
    private var bProjection: Broadcast[Projection[TC]],
    private var bExtractor: Broadcast[ValueExtractor[T]],
    private var bBinAggregator: Broadcast[Aggregator[T, U, V]]
  ) extends AccumulableParam[Array[U], (Int, Row)]() {

  def reset(
    newBProjection: Broadcast[Projection[TC]],
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

  override def addAccumulator(r: Array[U], t: (Int, Row)): Array[U] = {
    val index = t._1
    val row = t._2
    val current = r(index)
    Try({
      val value: Option[T] = bExtractor.value.rowToValue(row)
      r(index) = bBinAggregator.value.add(current, value)
    })
    r
  }

  override def addInPlace(r1: Array[U], r2: Array[U]): Array[U] = {
    val limit = bProjection.value.bins
    val binAggregator = bBinAggregator.value
    for (i <- 0 until limit) {
      r1(i) = binAggregator.merge(r1(i), r2(i))
    }
    r1
  }

  override def zero(initialValue: Array[U]): Array[U] = {
    makeBins(bProjection.value.bins, bBinAggregator.value.default)
  }
}
