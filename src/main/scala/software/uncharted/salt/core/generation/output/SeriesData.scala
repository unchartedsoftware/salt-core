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

package software.uncharted.salt.core.generation.output

import software.uncharted.salt.core.projection.Projection

/**
 * A thin wrapper class for the output of a tile generation
 *
 * @param coords the tile coordinates for this aggregator
 * @param bins the output values of bin aggregators
 * @param binsTouched the number of bins which have non-default values
 * @param tileMeta the output value of the tile aggregator
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam X Output data type for tile aggregators
 */
class SeriesData[TC, V, X](
  val coords: TC,
  val bins: Seq[V],
  val binsTouched: Int,
  val defaultBinValue: V,
  val tileMeta: Option[X],
  val projection: Projection[_,TC,_]) extends Serializable
