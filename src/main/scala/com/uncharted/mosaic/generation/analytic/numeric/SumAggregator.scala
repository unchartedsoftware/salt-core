package com.uncharted.mosaic.generation.analytic.numeric

import com.uncharted.mosaic.generation.analytic.Aggregator

object SumAggregator extends Aggregator[Double, Double, java.lang.Double] {
  def default(): Double = {
    0D
  }

  override def add(current: Double, next: Option[Double]): Double = {
    if (next.isDefined) {
      current + next.get
    } else {
      current
    }
  }
  override def merge(left: Double, right: Double): Double = {
    left + right
  }

  def finish(intermediate: Double): java.lang.Double = {
    intermediate
  }
}
