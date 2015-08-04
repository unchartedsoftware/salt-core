package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator
import com.uncharted.mosiac.generation.analytic.numeric._

//Track sums and counts separately, then finish by dividing
class MeanAggregator extends Aggregator[Double, (Long, Double), java.lang.Double] {
  def default(): (Long, Double) = {
    (CountAggregator.default, SumAggregator.default)
  }

  override def add(current: (Long, Double), next: Option[Double]): (Long, Double) = {
    (CountAggregator.add(current._1, next), SumAggregator.add(current._2, next))
  }
  override def merge(left: (Long, Double), right: (Long, Double)): (Long, Double) = {
    (CountAggregator.merge(left._1, right._1), SumAggregator.merge(left._2, right._2))
  }

  def finish(intermediate: (Long, Double)): java.lang.Double = {
    SumAggregator.finish(intermediate._2)/CountAggregator.finish(intermediate._1)
  }
}
