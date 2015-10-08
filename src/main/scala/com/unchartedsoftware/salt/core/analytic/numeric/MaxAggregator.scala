package com.unchartedsoftware.salt.core.analytic.numeric

import com.unchartedsoftware.salt.core.analytic.Aggregator

object MaxAggregator extends Aggregator[Double, Double, java.lang.Double] {
  def default(): Double = {
    Double.NaN
  }

  override def add(current: Double, next: Option[Double]): Double = {
    if (next.isDefined) {
      var fixedCurrent = current
      var fixedNext = next.get
      if (fixedCurrent.equals(Double.NaN)) {
        fixedCurrent = Double.MinValue
      }
      if (fixedNext.equals(Double.NaN)) {
        fixedNext = Double.MinValue
      }
      Math.max(fixedCurrent, fixedNext)
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
      Math.max(left, right)
    }
  }

  def finish(intermediate: Double): java.lang.Double = {
    intermediate
  }
}
