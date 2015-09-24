package com.unchartedsoftware.mosaic.core.projection.numeric

import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import org.apache.spark.sql.Row

/**
 * A projection into 2D cartesian (x,y) space
 *
 * @param minZoom the minimum zoom level which will be passed into rowToCoords()
 * @param maxZoom the maximum zoom level which will be passed into rowToCoords()
 * @param source the ValueExtractor which will extract numeric data-space coordinate values (x,y) from a Row
 * @param min the minimum value of a data-space coordinate (minX, minY)
 * @param max the maximum value of a data-space coordinate (maxX, maxY)
 */
class CartesianProjection(
  minZoom: Int,
  maxZoom: Int,
  source: ValueExtractor[(Double, Double)],
  min: (Double, Double),
  max: (Double, Double)) extends NumericProjection[(Int, Int, Int), (Int, Int), (Double, Double)](minZoom, maxZoom, source, min, max) {

  //Precompute some stuff we'll use frequently
  val _xRange = max._1 - min._1
  val _yRange = max._2 - min._2
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

  override def rowToCoords (r: Row, z: Int, maxBin: (Int, Int)): Option[((Int, Int, Int), (Int, Int))] = {
    if (z > maxZoom || z < minZoom) {
      throw new Exception("Requested zoom level is outside this projection's zoom bounds.")
    } else {
      //retrieve values from row
      val dCoords = source.rowToValue(r)

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
        var howFarX = scaledDataX * tileCounts(z)
        var howFarY = scaledDataY * tileCounts(z)
        var x = howFarX.toInt
        var y = howFarY.toInt
        var xBin = ((howFarX - x)*maxBin._1).toInt
        var yBin = (maxBin._2-1) - ((howFarY - y)*maxBin._2).toInt
        Some(((z, x, y), (xBin, yBin)))
      }
    }
  }

  override def binTo1D(bin: (Int, Int), maxBin: (Int, Int)): Int = {
    bin._1 + bin._2*maxBin._1
  }
}
