package com.uncharted.mosiac.generation

import org.apache.spark.{AccumulableParam, Accumulable}
import org.apache.spark.sql.Row

/**
 * Accumulator for a TileAggregator
 */
class TileGenerationAccumulable[T, U, V](val initialValue: TileAggregator[T, U, V])
  extends Accumulable[TileAggregator[T, U, V], ((Int, Int), Row)](initialValue, new TileGenerationAccumulableParam()) {
}

class TileGenerationAccumulableParam[T, U, V]()
  extends AccumulableParam[TileAggregator[T, U, V], ((Int, Int), Row)]() {

  override def addAccumulator(r: TileAggregator[T, U, V], t: ((Int, Int), Row)): TileAggregator[T, U, V] = {
    r.add(t._1, t._2)
  }

  override def addInPlace(r1: TileAggregator[T, U, V], r2: TileAggregator[T, U, V]): TileAggregator[T, U, V] = {
    r1.merge(r2)
    r1
  }

  override def zero(initialValue: TileAggregator[T, U, V]): TileAggregator[T, U, V] = {
    initialValue
  }
}
