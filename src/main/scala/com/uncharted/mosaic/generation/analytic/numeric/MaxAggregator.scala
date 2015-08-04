package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

object MaxAggregator extends Aggregator[Double, Double, java.lang.Double] {
  def default(): Double = {
    Double.MinValue
  }

  override def add(current: Double, next: Option[Double]): Double = {
    if (next.isDefined) {
      Math.max(current, next.get)
    } else {
      current
    }
  }
  override def merge(left: Double, right: Double): Double = {
    Math.max(left, right)
  }

  def finish(intermediate: Double): java.lang.Double = {
    intermediate
  }
}
