package com.unchartedsoftware.mosaic.core.projection.numeric

import org.apache.spark.sql.Row

/**
 * A projection into 2D mercator (lon,lat) space
 *
 * @param min the minimum value of a data-space coordinate (minLon, minLat)
 * @param max the maximum value of a data-space coordinate (maxLon, maxLat)
 */
class MercatorProjection(
  min: (Double, Double),
  max: (Double, Double)) extends NumericProjection[(Double, Double), (Int, Int, Int), (Int, Int)](min, max) {

  val _internalMaxX = Math.min(max._1, 180);
  val _internalMinX = Math.max(min._1, -180);
  val _internalMaxY = Math.min(max._2, 85.05112878);
  val _internalMinY = Math.max(min._2, -85.05112878);

  //Precompute some stuff we'll use frequently
  val piOver180 = Math.PI / 180;

  override def project (dCoords: Option[(Double, Double)], z: Int, maxBin: (Int, Int)): Option[((Int, Int, Int), (Int, Int))] = {
    //with help from https://developer.here.com/rest-apis/documentation/traffic/topics/mercator-projection.html
    if (!dCoords.isDefined) {
      None
    } else if (dCoords.get._1 >= max._1 || dCoords.get._1 <= min._1 || dCoords.get._2 >= max._2 || dCoords.get._2 <= min._2) {
      //make sure that we always stay INSIDE the range
      None
    } else {
      var lon = dCoords.get._1
      var lat = dCoords.get._2
      val latRad = (-lat) * piOver180;
      val n = Math.pow(2, z).toInt;
      val howFarX = n * ((lon + 180) / 360);
      val howFarY = n * (1-(Math.log(Math.tan(latRad) + 1/Math.cos(latRad)) / Math.PI)) / 2

      val x = howFarX.toInt
      val y = howFarY.toInt

      var xBin = ((howFarX - x)*(maxBin._1+1)).toInt
      var yBin = (maxBin._2) - ((howFarY - y)*(maxBin._2+1)).toInt

      Some(((z, x, y), (xBin, yBin)))
    }
  }

  override def binTo1D(bin: (Int, Int), maxBin: (Int, Int)): Int = {
    bin._1 + bin._2*(maxBin._1+1)
  }
}
