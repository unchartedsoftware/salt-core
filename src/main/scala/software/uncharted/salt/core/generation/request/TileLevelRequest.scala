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

package software.uncharted.salt.core.generation.request

import software.uncharted.salt.core.projection.Projection

/**
 * A TileRequest which takes the form of a Seq of zoom levels,
 * generating all tiles at those levels.
 */
class TileLevelRequest[TC](inLevels: Seq[Int], getZoomLevel: TC => Int) extends TileRequest[TC] {

  private val _levelMap = inLevels.map(c => (c, true)).toMap

  /**
   * @return true, if the given tile coordinate is part of this request. false otherwise.
   */
  def inRequest(tile: TC): Boolean = {
    _levelMap.contains(getZoomLevel(tile))
  }

  /**
   * @return a Seq of all zoom levels which contain tile coordinates in this request
   */
  def levels(): Seq[Int] = {
    inLevels
  }
}
