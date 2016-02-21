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

package software.uncharted.salt.core.analytic

/**
 * Implementations will provide functions for computing online
 * aggregations on values which fall within a bin, or within a tile.
 *
 * @tparam I input type
 * @tparam N intermediate type
 * @tparam O output type
 */
trait Aggregator[-I, N, O] extends Serializable {
  def default(): N
  def add(current: N, next: Option[I]): N
  def merge(left: N, right: N): N
  def finish(intermediate: N): O
}
