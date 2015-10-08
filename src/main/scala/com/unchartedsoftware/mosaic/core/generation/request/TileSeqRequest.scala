package com.unchartedsoftware.mosaic.core.generation.request

import com.unchartedsoftware.mosaic.core.projection.Projection

/**
 * A TileRequest which takes the form of a Seq of tile coordinates
 */
class TileSeqRequest[TC](tiles: Seq[TC], getZoomLevel: TC => Int) extends TileRequest[TC] {

  private val _levels = tiles.map(c => getZoomLevel(c)).distinct
  private val _tileMap = tiles.map(c => (c, true)).toMap

  /**
   * @return true, if the given tile coordinate is part of this request. false otherwise.
   */
  def inRequest(tile: TC): Boolean = {
    _tileMap.contains(tile)
  }

  /**
   * @return a Seq of all zoom levels which contain tile coordinates in this request
   */
  def levels(): Seq[Int] = {
    _levels
  }
}
