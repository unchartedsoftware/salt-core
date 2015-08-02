package com.uncharted.mosiac.generation.projection

import org.apache.spark.sql.Row

/**
 * @param xBins number of horizontal bins (tile width)
 * @param yBins number of vertical bins (tile height)
 */
abstract class Projection(val xBins: Int, val yBins: Int, val minZoom: Int, val maxZoom: Int) extends Serializable {

  /**
   * @param r the Row to retrieve data from
   * @param inCoords will fill this in  the tile/bin coordinates at every zoom level for a given Row, if the return value is true.
   * @return true if the given row is within the bounds of the viz. false otherwise.
   */
  def rowToCoords(r: Row, inCoords:Array[(Int, Int, Int, Int, Int)]): Boolean
}
