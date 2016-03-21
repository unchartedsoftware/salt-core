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

import software.uncharted.salt.core.projection.Projection

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
 * @tparam TC the abstract type representing a tile coordinate.
 * @tparam BC the abstract type representing a bin coordinate. Must be something that can be represented in 1 dimension.
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam X Output data type for tile aggregators
 */
class SeriesData[
  TC,
  BC,
  @specialized(Int, Long, Double) V,
  @specialized(Int, Long, Double) X
] private[salt] (
  private[salt] val projection: Projection[_, TC, BC],
  private[salt] val maxBin: BC,
  val coords: TC,
  val bins: Seq[V],
  val binsTouched: Int,
  val defaultBinValue: V,
  val tileMeta: Option[X]) extends Serializable {

  /**
   * Retrieve a value from this SeriesData's bins using a bin coordinate
   * @param bin A bin coordinate
   * @return the corresponding value
   */
  def apply(bin: BC): V = {
    bins(projection.binTo1D(maxBin, bin))
  }

  /**
   * Combine this SeriesData with another congruent one. Congruent SeriesData have an identical
   * tile coordinate and the same bin dimensions.
   *
   * @param other the SeriesData to merge with
   * @param binMerge the function for merging bin values
   * @param tileMetaMerge  the Optional function for merging tile metadata
   * @tparam OV other's bin value type
   * @tparam OX other's tile metadata type
   * @tparam NV output bin value type
   * @tparam NX output tile metadata type
   */
  @throws(classOf[SeriesDataMergeException])
  def merge[OV, OX, NV, NX](
    other: SeriesData[TC, BC, OV, OX],
    binMerge: (V, OV) => NV,
    tileMetaMerge: Option[(X, OX) => NX] = None
  ): SeriesData[TC, BC, NV, NX] = {
    if (other.bins.length != bins.length) {
      throw new SeriesDataMergeException("Two Series with different bin dimensions cannot be merged.")
    } else if (!coords.equals(other.coords)) {
      throw new SeriesDataMergeException("SeriesData with nonmatching tile coordinates cannot be merged.")
    }

    // new default value
    val newDefault = binMerge(defaultBinValue, other.defaultBinValue)

    // merge bins
    var newBinsTouched = 0
    val newBins = bins.zip(other.bins).map(v => {
      val result = binMerge(v._1, v._2)
      if (!result.equals(newDefault)) {
        newBinsTouched += 1
      }
      result
    })

    // compute new meta
    val newMeta: Option[NX] = tileMetaMerge.isDefined match {
      case true => tileMeta.zip(other.tileMeta).map(m =>  tileMetaMerge.get(m._1, m._2)).headOption
      case _ => None
    }

    new SeriesData(projection, maxBin, coords, newBins, newBinsTouched, newDefault, newMeta)
  }
}
