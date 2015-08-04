package com.uncharted.mosiac.generation

import org.apache.spark.{AccumulableParam, Accumulable}
import org.apache.spark.sql.Row

/**
 * Accumulator for a TileBuilder
 */
class TileGenerationAccumulableParam[T, U, V, W, X]()
  extends AccumulableParam[TileBuilder[T, U, V, W, X], ((Int, Int), Row)]() {

  override def addAccumulator(r: TileBuilder[T, U, V, W, X], t: ((Int, Int), Row)): TileBuilder[T, U, V, W, X] = {
    r.add(t._1, t._2)
  }

  override def addInPlace(r1: TileBuilder[T, U, V, W, X], r2: TileBuilder[T, U, V, W, X]): TileBuilder[T, U, V, W, X] = {
    r1.merge(r2)
    r1
  }

  override def zero(initialValue: TileBuilder[T, U, V, W, X]): TileBuilder[T, U, V, W, X] = {
    initialValue
  }
}
