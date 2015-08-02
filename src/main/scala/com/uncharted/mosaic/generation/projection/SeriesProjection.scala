package com.uncharted.mosiac.generation.projection

import com.uncharted.mosaic.util.DataFrameUtil
import org.apache.spark.sql.Row

class SeriesProjection(
  xBins: Int,
  minZoom: Int,
  maxZoom: Int,
  val xCol: Int,
  val maxX: Double,
  val minX: Double) extends Projection(xBins, 1, minZoom, maxZoom) {

  //Precompute some stuff we'll use frequently
  val _range = maxX - minX
  //number of tiles at each zoom level
  val tileCounts = new Array[Int](maxZoom+1)
  for (i <- minZoom until maxZoom+1) {
    tileCounts(i) = 1 << i //Math.pow(2, i).toInt
  }
  //width of a tile in data space at each zoom level
  val tileWidths = tileCounts.map(a => _range/a)

  override def rowToCoords (r: Row, inCoords: Array[(Int, Int, Int, Int, Int)]): Boolean = {
    //convert value to a double
    val doubleX = DataFrameUtil.getDouble(xCol, r)

    if (doubleX > maxX || doubleX < minX) {
      false
    } else {
      val translatedDataX = doubleX - minX
      //scale it to [0,1]
      val scaledDataX = translatedDataX/_range

      //compute all tile/bin coordinates (z, x, y, bX, bY)
      var x = 0
      var xBin = 0
      var howFarX = 0D
      for (i <- 0 until maxZoom+1) {
        howFarX = scaledDataX * tileCounts(i)
        x = howFarX.toInt
        xBin = ((howFarX - x)*xBins).toInt
        inCoords(i) = (i, x, 0, xBin, 0)
      }
      true
    }
  }
}
