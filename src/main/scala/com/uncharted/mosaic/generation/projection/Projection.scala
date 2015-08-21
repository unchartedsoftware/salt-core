package com.uncharted.mosaic.generation.projection

import org.apache.spark.sql.Row

/**
 * @param xBins number of horizontal bins (tile width)
 * @param yBins number of vertical bins (tile height)
 * @param minZoom the minimum zoom level which will be passed into rowToCoords()
 * @param maxZoom the maximum zoom level which will be passed into rowToCoords()
 */
abstract class Projection(val xBins: Int, val yBins: Int, val minZoom: Int, val maxZoom: Int) extends Serializable {

  /**
   * @param r the Row to retrieve data from
   * @param z the zoom level
   * @param inCoords will fill this in with the tile/bin coordinates, if the return value is true
   * @return true if the given row is within the bounds of the viz. false otherwise.
   */
  def rowToCoords(r: Row, z: Int, inCoords: Array[Int]): Boolean
}
