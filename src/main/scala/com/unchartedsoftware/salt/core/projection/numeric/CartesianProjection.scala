package com.unchartedsoftware.salt.core.projection.numeric

import org.apache.spark.sql.Row

/**
 * A projection into 2D cartesian (x,y) space
 *
 * @param min the minimum value of a data-space coordinate (minX, minY)
 * @param max the maximum value of a data-space coordinate (maxX, maxY)
 */
class CartesianProjection(
  min: (Double, Double),
  max: (Double, Double)) extends NumericProjection[(Double, Double), (Int, Int, Int), (Int, Int)](min, max) {

  //Precompute some stuff we'll use frequently
  val _xRange = max._1 - min._1
  val _yRange = max._2 - min._2

  override def project (dCoords: Option[(Double, Double)], z: Int, maxBin: (Int, Int)): Option[((Int, Int, Int), (Int, Int))] = {
    if (!dCoords.isDefined) {
      None
    } else if (dCoords.get._1 >= max._1 || dCoords.get._1 <= min._1 || dCoords.get._2 >= max._2 || dCoords.get._2 <= min._2) {
      //make sure that we always stay INSIDE the range
      None
    } else {
      val translatedDataX = dCoords.get._1 - min._1
      val translatedDataY = dCoords.get._2 - min._2
      //scale it to [0,1)
      val scaledDataX = translatedDataX / _xRange
      val scaledDataY = translatedDataY / _yRange

      //compute tile/bin coordinates (z, x, y, bX, bY)
      val n = Math.pow(2, z).toInt;
      var howFarX = scaledDataX * n
      var howFarY = scaledDataY * n
      var x = howFarX.toInt
      var y = howFarY.toInt
      var xBin = ((howFarX - x)*(maxBin._1+1)).toInt
      var yBin = (maxBin._2) - ((howFarY - y)*(maxBin._2+1)).toInt
      Some(((z, x, y), (xBin, yBin)))
    }
  }

  override def binTo1D(bin: (Int, Int), maxBin: (Int, Int)): Int = {
    bin._1 + bin._2*(maxBin._1+1)
  }
}
