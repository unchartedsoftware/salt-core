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

object SumAggregator extends Aggregator[Double, Double, Double] {
  def default(): Double = {
    0D
  }

  override def add(current: Double, next: Option[Double]): Double = {
    if (next.isDefined && !next.get.equals(Double.NaN)) {
      current + next.get
    } else {
      current
    }
  }
  override def merge(left: Double, right: Double): Double = {
    left + right
  }

  def finish(intermediate: Double): Double = {
    intermediate
  }
}
