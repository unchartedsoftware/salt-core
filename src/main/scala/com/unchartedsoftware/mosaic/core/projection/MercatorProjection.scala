package com.unchartedsoftware.mosaic.core.projection

import com.unchartedsoftware.mosaic.core.util.DataFrameUtil
import org.apache.spark.sql.Row

class MercatorProjection(
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

  val _internalMaxX = Math.min(maxX, 180);
  val _internalMinX = Math.max(minX, -180);
  val _internalMaxY = Math.min(maxY, 85.05112878);
  val _internalMinY = Math.max(minY, -85.05112878);

  //Precompute some stuff we'll use frequently
  val piOver180 = Math.PI / 180;

  //number of tiles at each zoom level
  val tileCounts = new Array[Int](maxZoom+1)
  for (i <- minZoom until maxZoom+1) {
    tileCounts(i) = 1 << i //Math.pow(2, i).toInt
  }

  override def getZoomLevel(c: (Int, Int, Int)): Int = {
    c._1
  }

  override def rowToCoords (r: Row, z: Int): Option[((Int, Int, Int), Int)] = {
    if (z > maxZoom || z < minZoom) {
      throw new Exception("Requested zoom level is outside this projection's zoom bounds.")
    } else {
      //with help from https://developer.here.com/rest-apis/documentation/traffic/topics/mercator-projection.html

      //convert value to a double
      val lon = DataFrameUtil.getDouble(xCol, r)
      val lat = DataFrameUtil.getDouble(yCol, r)

      //make sure that we always stay INSIDE the range
      if (lon >= maxX || lon <= minX || lat >= maxY || lat <= minY) {
        None
      } else {
        val latRad = (-lat) * piOver180;
        val n = tileCounts(z);
        val howFarX = n * ((lon + 180) / 360);
        val howFarY = n * (1-(Math.log(Math.tan(latRad) + 1/Math.cos(latRad)) /
        Math.PI)) / 2

        val x = howFarX.toInt
        val y = howFarY.toInt

        var xBin = ((howFarX - x)*xBins).toInt
        var yBin = (yBins-1) - ((howFarY - y)*yBins).toInt

        Some(((z, x, y), xBin + yBin*xBins))
      }
    }
  }
}
