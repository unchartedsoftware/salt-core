package com.uncharted.mosiac.generation

import com.uncharted.mosiac.generation.analytic.{Aggregator, ValueExtractor}
import com.uncharted.mosiac.generation.projection.Projection
import scala.reflect.ClassTag
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.{AccumulableParam, Accumulable}
import org.apache.spark.sql.Row

/**
 * Accumulator which aggregates bin values for a tile
 * @tparam T Input data type for aggregators
 * @tparam U Intermediate data type for bin aggregators
 */
class TileGenerationAccumulableParam[T, U: ClassTag](
    bProjection: Broadcast[Projection],
    bExtractor: Broadcast[ValueExtractor[T]],
    bBinAggregator: Broadcast[Aggregator[T, U, _]]
  ) extends AccumulableParam[Array[U], ((Int, Int), Row)]() {

  //will store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }

  override def addAccumulator(r: Array[U], t: ((Int, Int), Row)): Array[U] = {
    val bin = t._1
    val row = t._2
    val value: Option[T] = bExtractor.value.rowToValue(row)
    val index = bin._1 + bin._2*bProjection.value.xBins
    val current = r(index)
    r(index) = bBinAggregator.value.add(current, value)
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
