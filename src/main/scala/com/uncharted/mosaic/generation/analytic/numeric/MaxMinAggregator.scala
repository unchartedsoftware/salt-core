package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator
import com.uncharted.mosiac.generation.analytic.numeric._

object MaxMinAggregator  extends Aggregator[Double, (Double, Double), (java.lang.Double, java.lang.Double)] {
  def default(): (Double, Double) = {
    (MinAggregator.default, MaxAggregator.default)
  }

  override def add(current: (Double, Double), next: Option[Double]): (Double, Double) = {
    (MinAggregator.add(current._1, next), MaxAggregator.add(current._2, next))
  }
  override def merge(left: (Double, Double), right: (Double, Double)): (Double, Double) = {
    (MinAggregator.merge(left._1, right._1), MaxAggregator.merge(left._2, right._2))
  }

  def finish(intermediate: (Double, Double)): (java.lang.Double, java.lang.Double) = {
    (MinAggregator.finish(intermediate._1), MaxAggregator.finish(intermediate._2))
  }
}
