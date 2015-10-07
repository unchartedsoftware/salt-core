package com.unchartedsoftware.mosaic.core.analytic.numeric

import com.unchartedsoftware.mosaic.core.analytic.Aggregator
import com.unchartedsoftware.mosaic.core.analytic.numeric._

object MinMaxAggregator extends Aggregator[java.lang.Double, (Double, Double), (java.lang.Double, java.lang.Double)] {
  def default(): (Double, Double) = {
    (MinAggregator.default, MaxAggregator.default)
  }

  override def add(current: (Double, Double), next: Option[java.lang.Double]): (Double, Double) = {
    if (next.isDefined) {
      (MinAggregator.add(current._1, Some(next.get.doubleValue)), MaxAggregator.add(current._2, Some(next.get.doubleValue)))
    } else {
      (MinAggregator.add(current._1, None), MaxAggregator.add(current._2, None))
    }
  }
  override def merge(left: (Double, Double), right: (Double, Double)): (Double, Double) = {
    (MinAggregator.merge(left._1, right._1), MaxAggregator.merge(left._2, right._2))
  }

  def finish(intermediate: (Double, Double)): (java.lang.Double, java.lang.Double) = {
    (MinAggregator.finish(intermediate._1), MaxAggregator.finish(intermediate._2))
  }
}
