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

package software.uncharted.salt.core.generation.output

/**
 * A thin wrapper class for the output of a tile generation,
 * for a single Series
 *
 * @param coords the tile coordinates for this aggregator
 * @param bins the output values of bin aggregators
 * @param binsTouched the number of bins which have non-default values. If this value is low, clients might want to
 *                    consider a sparse representation of this tile when serializing.
 * @param defaultBinValue the default value for bins in this tile. If many bin values in bins equals this value, clients
 *                        might want to consider a sparse representation of this tile when serializing
 * @param tileMeta the output value of the tile aggregator
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam X Output data type for tile aggregators
 */
class SeriesData[
  TC,
  @specialized(Int, Long, Double) V,
  @specialized(Int, Long, Double) X
](
  val coords: TC,
  val bins: Seq[V],
  val binsTouched: Int,
  val defaultBinValue: V,
  val tileMeta: Option[X]) extends Serializable
