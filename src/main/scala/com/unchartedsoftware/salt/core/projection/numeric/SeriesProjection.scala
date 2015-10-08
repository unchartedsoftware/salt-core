package com.unchartedsoftware.salt.core.projection.numeric

import org.apache.spark.sql.Row

/**
 * A projection into 1D (x) space
 *
 * @param min the minimum value of a data-space coordinate (minX)
 * @param max the maximum value of a data-space coordinate (maxX)
 */
class SeriesProjection(
  min: Double,
  max: Double) extends NumericProjection[Double, (Int, Int), Int](min, max) {

  //Precompute some stuff we'll use frequently
  val _range = max - min

  override def project (xValue: Option[Double], z: Int, maxBin: Int): Option[((Int, Int), Int)] = {
    if (!xValue.isDefined) {
      None
    } else if (xValue.get >= max || xValue.get <= min) {
      None
    } else {
      val translatedDataX = xValue.get - min
      //scale it to [0,1]
      val scaledDataX = translatedDataX/_range

      //compute all tile/bin coordinates (z, x, y, bX, bY)
      val n = Math.pow(2, z).toInt;
      var howFarX = scaledDataX * n
      var x = howFarX.toInt
      var xBin = ((howFarX - x)*(maxBin+1)).toInt
      Some(((z, x), xBin))
    }
  }

  override def binTo1D(bin: Int, maxBin: Int): Int = {
    bin
  }
}
