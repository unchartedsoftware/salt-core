package com.uncharted.mosaic.generation.projection

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

  override def rowToCoords (r: Row, z: Int, inCoords: Array[Int]): Boolean = {
    if (z > maxZoom || z < minZoom) {
      throw new Exception("Requested zoom level is outside this projection's zoom bounds.")
    }
    //convert value to a double
    val doubleX = DataFrameUtil.getDouble(xCol, r)

    if (doubleX > maxX || doubleX < minX) {
      false
    } else {
      val translatedDataX = doubleX - minX
      //scale it to [0,1]
      val scaledDataX = translatedDataX/_range

      //compute all tile/bin coordinates (z, x, y, bX, bY)
      var howFarX = scaledDataX * tileCounts(z)
      var x = howFarX.toInt
      var xBin = ((howFarX - x)*xBins).toInt
      inCoords(0) = z
      inCoords(1) = x
      inCoords(2) = 0
      inCoords(3) = xBin
      inCoords(4) = 0

      true
    }
  }
}
