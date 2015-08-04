package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator
import com.uncharted.mosiac.generation.analytic.numeric._

//Track sums and counts separately, then finish by dividing
class MeanAggregator extends Aggregator[Double, (Double, Double), java.lang.Double] {
  def default(): (Double, Double) = {
    (CountAggregator.default, SumAggregator.default)
  }

  override def add(current: (Double, Double), next: Option[Double]): (Double, Double) = {
    (CountAggregator.add(current._1, next), SumAggregator.add(current._2, next))
  }
  override def merge(left: (Double, Double), right: (Double, Double)): (Double, Double) = {
    (CountAggregator.merge(left._1, right._1), SumAggregator.merge(left._2, right._2))
  }

  def finish(intermediate: (Double, Double)): java.lang.Double = {
    SumAggregator.finish(intermediate._2)/CountAggregator.finish(intermediate._1)
  }
}
