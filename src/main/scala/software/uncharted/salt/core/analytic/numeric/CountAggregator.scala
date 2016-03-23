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
 * Useful for counting records or elements within a record,
 * where the count for an individual record is passed in as
 * an Int
 */
object CountAggregator extends Aggregator[Int, Double, Double] {

  def default(): Double = {
    0L
  }

  override def add(current: Double, next: Option[Int]): Double = {
    current + next.getOrElse(0)
  }

  override def merge(left: Double, right: Double): Double = {
    left + right
  }

  def finish(intermediate: Double): Double = {
    intermediate
  }
}
