package com.unchartedsoftware.mosaic.core.projection

import org.apache.spark.sql.Row

/**
 * @param bins number of bins
 * @param minZoom the minimum zoom level which will be passed into rowToCoords()
 * @param maxZoom the maximum zoom level which will be passed into rowToCoords()
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 */
abstract class Projection[TC](
  val bins: Int,
  val minZoom: Int,
  val maxZoom: Int
) extends Serializable {

  /**
   * @param c a tile coordinate
   * @return the zoom level of the coordinate
   */
  def getZoomLevel(c: TC): Int

  /**
   * @param r the Row to retrieve data from
   * @param z the zoom level
   * @return Some[(TC, Int)] representing the tile coordinate and 1D bin index if the given row is within the bounds of the viz. None otherwise.
   */
  def rowToCoords(r: Row, z: Int): Option[(TC, Int)]
}
