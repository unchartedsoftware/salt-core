package com.uncharted.mosaic.generation.projection

import com.uncharted.mosaic.util.DataFrameUtil
import org.apache.spark.sql.Row

class SpatialProjection(xBins: Int,
                        yBins: Int,
                        minZoom: Int,
                        maxZoom: Int,
                        val xCol: Int,
                        val maxX: Double,
                        val minX: Double,
                        val yCol: Int,
                        val maxY: Double,
                        val minY: Double) extends Projection(xBins, yBins, minZoom, maxZoom) {

  //Precompute some stuff we'll use frequently
  val _xRange = maxX - minX
  val _yRange = maxY - minY
  //number of tiles at each zoom level
  val tileCounts = new Array[Int](maxZoom+1)
  for (i <- minZoom until maxZoom+1) {
    tileCounts(i) = 1 << i //Math.pow(2, i).toInt
  }
  //width of a tile in data space at each zoom level
  val tileWidths = tileCounts.map(a => _xRange/a)
  val tileHeights = tileCounts.map(a => _yRange/a)

  override def rowToCoords (r: Row, z: Int, inCoords: Array[Int]): Boolean = {
    if (z > maxZoom || z < minZoom) {
      throw new Exception("Requested zoom level is outside this projection's zoom bounds.")
    }

    //convert value to a double
    val doubleX = DataFrameUtil.getDouble(xCol, r)
    val doubleY = DataFrameUtil.getDouble(yCol, r)

    //make sure that we always stay INSIDE the range
    if (doubleX >= maxX || doubleX <= minX || doubleY >= maxY || doubleY <= minY) {
      false
    } else {
      val translatedDataX = doubleX - minX
      val translatedDataY = doubleY - minY
      //scale it to [0,1)
      val scaledDataX = translatedDataX / _xRange
      val scaledDataY = translatedDataY / _yRange

      //compute tile/bin coordinates (z, x, y, bX, bY)
      var howFarX = scaledDataX * tileCounts(z)
      var howFarY = scaledDataY * tileCounts(z)
      var x = howFarX.toInt
      var y = howFarY.toInt
      var xBin = ((howFarX - x)*xBins).toInt
      var yBin = (yBins-1) - ((howFarY - y)*yBins).toInt
      inCoords(0) = z
      inCoords(1) = x
      inCoords(2) = y
      inCoords(3) = xBin
      inCoords(4) = yBin

      true
    }
  }
}
