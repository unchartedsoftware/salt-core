package com.unchartedsoftware.mosaic.core.projection.numeric

import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import org.apache.spark.sql.Row

class MercatorProjection(
  val xBins: Int,
  val yBins: Int,
  minZoom: Int,
  maxZoom: Int,
  source: ValueExtractor[(Double, Double)],
  min: (Double, Double),
  max: (Double, Double)) extends NumericProjection[(Int, Int, Int), (Double, Double)](xBins*yBins, minZoom, maxZoom, source, min, max) {

  val _internalMaxX = Math.min(max._1, 180);
  val _internalMinX = Math.max(min._1, -180);
  val _internalMaxY = Math.min(max._2, 85.05112878);
  val _internalMinY = Math.max(min._2, -85.05112878);

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

      //retrieve values from row
      val dCoords = source.rowToValue(r)

      if (!dCoords.isDefined) {
        None
      } else if (dCoords.get._1 >= max._1 || dCoords.get._1 <= min._1 || dCoords.get._2 >= max._2 || dCoords.get._2 <= min._2) {
        //make sure that we always stay INSIDE the range
        None
      } else {
        var lon = dCoords.get._1
        var lat = dCoords.get._2
        val latRad = (-lat) * piOver180;
        val n = tileCounts(z);
        val howFarX = n * ((lon + 180) / 360);
        val howFarY = n * (1-(Math.log(Math.tan(latRad) + 1/Math.cos(latRad)) / Math.PI)) / 2

        val x = howFarX.toInt
        val y = howFarY.toInt

        var xBin = ((howFarX - x)*xBins).toInt
        var yBin = (yBins-1) - ((howFarY - y)*yBins).toInt

        Some(((z, x, y), xBin + yBin*xBins))
      }
    }
  }
}
