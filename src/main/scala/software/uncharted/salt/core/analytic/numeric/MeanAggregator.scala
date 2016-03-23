/*
 * Copyright 2016 Uncharted Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.uncharted.salt.core.analytic.numeric

import software.uncharted.salt.core.analytic.Aggregator

//Track sums and counts separately, then finish by dividing
object MeanAggregator extends Aggregator[Double, (Double, Double), Double] {
  def default(): (Double, Double) = {
    (CountAggregator.default, SumAggregator.default)
  }

  override def add(current: (Double, Double), next: Option[Double]): (Double, Double) = {
    (CountAggregator.add(current._1, Some(1)), SumAggregator.add(current._2, next))
  }

  override def merge(left: (Double, Double), right: (Double, Double)): (Double, Double) = {
    (CountAggregator.merge(left._1, right._1), SumAggregator.merge(left._2, right._2))
  }

  def finish(intermediate: (Double, Double)): Double = {
    SumAggregator.finish(intermediate._2)/CountAggregator.finish(intermediate._1)
  }
}
