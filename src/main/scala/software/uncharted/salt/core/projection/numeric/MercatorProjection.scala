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
 * A projection into 2D mercator (lon,lat) space
 *
 * @param min the minimum value of a data-space coordinate (minLon, minLat)
 * @param max the maximum value of a data-space coordinate (maxLon, maxLat)
 * @param zoomLevels the TMS/WMS zoom levels to project into
 */
class MercatorProjection(
  zoomLevels: Seq[Int],
  min: (Double, Double) = (-180, -85.05112878),
  max: (Double, Double) = (180, 85.05112878),
  tms: Boolean = true) extends NumericProjection[(Double, Double), (Int, Int, Int), (Int, Int)](min, max) {

  val _internalMaxX = Math.min(max._1, 180);
  val _internalMinX = Math.max(min._1, -180);
  val _internalMaxY = Math.min(max._2, 85.05112878);
  val _internalMinY = Math.max(min._2, -85.05112878);

  //Precompute some stuff we'll use frequently
  val piOver180 = Math.PI / 180;

  override def project (dCoords: Option[(Double, Double)], maxBin: (Int, Int)): Option[Seq[((Int, Int, Int), (Int, Int))]] = {
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

      Some(zoomLevels.map(z => {
        val n = Math.pow(2, z).toInt;

        val howFarX = n * ((lon + 180) / 360);
        val howFarY = n * (1 + (Math.log(Math.tan(latRad) + 1/Math.cos(latRad)) / Math.PI)) / 2

        val x = howFarX.toInt
        val y = howFarY.toInt

        val xBin = ((howFarX - x)*(maxBin._1 + 1)).toInt
        val yBin = ((howFarY - y)*(maxBin._2 + 1)).toInt

        // If TMS system, flip y axis
        val yCoord = if (tms) n - 1 - y else y
        ((z, x, yCoord), (xBin, yBin))
      }))
    }
  }

  override def binTo1D(bin: (Int, Int), maxBin: (Int, Int)): Int = {
    bin._1 + bin._2*(maxBin._1 + 1)
  }
}
