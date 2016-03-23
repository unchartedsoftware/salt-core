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

/**
 * Useful for calculating the minimum value across values
 * derived from source records
 */
object MinAggregator extends Aggregator[Double, Double, Double] {
  def default(): Double = {
    Double.NaN
  }

  override def add(current: Double, next: Option[Double]): Double = {
    if (next.isDefined) {
      var fixedCurrent = current
      var fixedNext = next.get
      if (fixedCurrent.equals(Double.NaN)) {
        fixedCurrent = Double.MaxValue
      }
      if (fixedNext.equals(Double.NaN)) {
        fixedNext = Double.MaxValue
      }
      Math.min(fixedCurrent, fixedNext)
    } else {
      current
    }
  }
  override def merge(left: Double, right: Double): Double = {
    if (left.equals(Double.NaN)) {
      right
    } else if (right.equals(Double.NaN)) {
      left
    } else {
      Math.min(left, right)
    }
  }

  def finish(intermediate: Double): Double = {
    intermediate
  }
}
