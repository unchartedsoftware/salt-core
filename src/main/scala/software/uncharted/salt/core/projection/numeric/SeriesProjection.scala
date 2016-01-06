/*
 * Copyright 2015 Uncharted Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.uncharted.salt.core.projection.numeric

import org.apache.spark.sql.Row

/**
 * A projection into zooming 1D (z,x) space
 *
 * @param min the minimum value of a data-space coordinate (minX)
 * @param max the maximum value of a data-space coordinate (maxX)
 * @param zoomLevels the TMS/WMS zoom levels to project into
 */
class SeriesProjection(
  zoomLevels: Seq[Int],
  min: Double,
  max: Double
) extends NumericProjection[Double, (Int, Int), Int](min, max) {

  //Precompute some stuff we'll use frequently
  val _range = max - min

  override def project (xValue: Option[Double], maxBin: Int): Option[Seq[((Int, Int), Int)]] = {
    if (!xValue.isDefined) {
      None
    } else if (xValue.get >= max || xValue.get <= min) {
      None
    } else {
      val translatedDataX = xValue.get - min
      //scale it to [0,1]
      val scaledDataX = translatedDataX/_range

      //compute all tile/bin coordinates (z, x, bX)
      Some(zoomLevels.map(z => {
        val n = Math.pow(2, z).toInt;
        val howFarX = scaledDataX * n
        val x = howFarX.toInt
        val xBin = ((howFarX - x)*(maxBin + 1)).toInt
        ((z, x), xBin)
      }))
    }
  }

  override def binTo1D(bin: Int, maxBin: Int): Int = {
    bin
  }
}
