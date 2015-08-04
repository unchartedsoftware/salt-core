package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

object MinAggregator extends Aggregator[Double, Double, java.lang.Double] {
  def default(): Double = {
    Double.MaxValue
  }

  override def add(current: Double, next: Option[Double]): Double = {
    if (next.isDefined) {
      Math.min(current, next.get)
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
