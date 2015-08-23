package com.unchartedsoftware.mosaic.core.analytic.numeric

import com.unchartedsoftware.mosaic.core.analytic.Aggregator

object MinAggregator extends Aggregator[Double, Double, java.lang.Double] {
  def default(): Double = {
    Double.NaN
  }

  override def add(current: Double, next: Option[Double]): Double = {
    if (next.isDefined) {
      var fixedCurrent = current
      var fixedNext = next.get
      if (fixedCurrent.equals(Double.NaN)) {
        fixedCurrent = Double.MaxValue
      }
      if (fixedNext.equals(Double.NaN)) {
        fixedNext = Double.MaxValue
      }
      Math.min(fixedCurrent, fixedNext)
    } else {
      current
    }
  }
  override def merge(left: Double, right: Double): Double = {
    if (left.equals(Double.NaN)) {
      right
    } else if (right.equals(Double.NaN)) {
      left
    } else {
      Math.min(left, right)
    }
  }

  def finish(intermediate: Double): java.lang.Double = {
    intermediate
  }
}
