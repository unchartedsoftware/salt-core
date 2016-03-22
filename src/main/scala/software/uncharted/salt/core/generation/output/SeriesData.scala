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
import software.uncharted.salt.core.util.SparseArray

import scala.reflect.ClassTag

/**
 * A thin wrapper class for the output of a tile generation,
 * for a single Series
 *
 * @param coords the tile coordinates for this aggregator
 * @param bins the output values of bin aggregators
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
  val bins: SparseArray[V],
  val tileMeta: Option[X]
) extends Serializable {

  /**
   * Retrieve a value from this SeriesData's bins using a bin coordinate
   * @param bin A bin coordinate
   * @return the corresponding value
   */
  def apply(bin: BC): V = {
    bins(projection.binTo1D(bin, maxBin))
  }

  /**
   * Combine this SeriesData with another congruent one. Congruent SeriesData have an identical
   * tile coordinate and the same bin dimensions. The new SeriesData will inherit this SeriesData's
   * Projection.
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
  def merge[OV, OX, NV: ClassTag, NX](
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
    val newDefault = binMerge(bins.default, other.bins.default)

    // merge bins
    val newBins = new SparseArray[NV](0, newDefault)
    while (newBins.length<bins.length) {
      newBins += binMerge(bins(newBins.length), other.bins(newBins.length))
    }

    // compute new meta
    var newMeta: Option[NX] = None
    if  (tileMetaMerge.isDefined && tileMeta.isDefined && other.tileMeta.isDefined) {
      newMeta = Some(tileMetaMerge.get(tileMeta.get, other.tileMeta.get))
    }

    new SeriesData(projection, maxBin, coords, newBins, newMeta)
  }
}
