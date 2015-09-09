package com.unchartedsoftware.mosaic.core.projection

import com.unchartedsoftware.mosaic.util.DataFrameUtil
import org.apache.spark.sql.Row

class CartesianProjection(
  val xBins: Int,
  val yBins: Int,
  minZoom: Int,
  maxZoom: Int,
  val xCol: Int,
  val maxX: Double,
  val minX: Double,
  val yCol: Int,
  val maxY: Double,
  val minY: Double) extends Projection[(Int, Int, Int)](xBins*yBins, minZoom, maxZoom) {

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

  override def getZoomLevel(c: (Int, Int, Int)): Int = {
    c._1
  }

  override def rowToCoords (r: Row, z: Int): Option[((Int, Int, Int), Int)] = {
    if (z > maxZoom || z < minZoom) {
      throw new Exception("Requested zoom level is outside this projection's zoom bounds.")
    } else {
      //convert value to a double
      val doubleX = DataFrameUtil.getDouble(xCol, r)
      val doubleY = DataFrameUtil.getDouble(yCol, r)

      //make sure that we always stay INSIDE the range
      if (doubleX >= maxX || doubleX <= minX || doubleY >= maxY || doubleY <= minY) {
        None
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
        Some(((z, x, y), xBin + yBin*xBins))
      }
    }
  }
}
