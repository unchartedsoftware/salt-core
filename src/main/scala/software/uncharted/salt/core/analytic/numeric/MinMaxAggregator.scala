/*
 * Copyright 2015 Uncharted Software Inc.
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
import software.uncharted.salt.core.analytic.numeric._

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
