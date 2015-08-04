package com.uncharted.mosaic.generation.analytic.numeric

import com.uncharted.mosaic.generation.analytic.Aggregator

object MinAggregator extends Aggregator[Double, Double, java.lang.Double] {
  def default(): Double = {
    Double.NaN
  }

  override def add(current: Double, next: Option[Double]): Double = {
    if (next.isDefined) {
      var fixedCurrent = current
      if (current == Double.NaN) fixedCurrent = Double.MaxValue
      Math.max(fixedCurrent, next.get)
    } else {
      current
    }
  }
  override def merge(left: Double, right: Double): Double = {
    Math.min(left, right)
  }

  def finish(intermediate: Double): java.lang.Double = {
    intermediate
  }
}
