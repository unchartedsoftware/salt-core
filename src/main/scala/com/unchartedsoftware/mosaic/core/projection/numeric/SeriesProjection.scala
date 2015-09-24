package com.unchartedsoftware.mosaic.core.projection.numeric

import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import org.apache.spark.sql.Row

/**
 * A projection into 1D (x) space
 *
 * @param minZoom the minimum zoom level which will be passed into rowToCoords()
 * @param maxZoom the maximum zoom level which will be passed into rowToCoords()
 * @param source the ValueExtractor which will extract numeric data-space coordinate values (x) from a Row
 * @param min the minimum value of a data-space coordinate (minX)
 * @param max the maximum value of a data-space coordinate (maxX)
 */
class SeriesProjection(
  minZoom: Int,
  maxZoom: Int,
  source: ValueExtractor[Double],
  min: Double,
  max: Double) extends NumericProjection[(Int, Int), Int, Double](minZoom, maxZoom, source, min, max) {

  //Precompute some stuff we'll use frequently
  val _range = max - min
  //number of tiles at each zoom level
  val tileCounts = new Array[Int](maxZoom+1)
  for (i <- minZoom until maxZoom+1) {
    tileCounts(i) = 1 << i //Math.pow(2, i).toInt
  }
  //width of a tile in data space at each zoom level
  val tileWidths = tileCounts.map(a => _range/a)

  override def getZoomLevel(c: (Int, Int)): Int = {
    c._1
  }

  override def rowToCoords (r: Row, z: Int, maxBin: Int): Option[((Int, Int), Int)] = {
    if (z > maxZoom || z < minZoom) {
      throw new Exception("Requested zoom level is outside this projection's zoom bounds.")
    } else {
      //retrieve value from row
      val xValue = source.rowToValue(r)
      if (!xValue.isDefined) {
        None
      } else if (xValue.get >= max || xValue.get <= min) {
        None
      } else {
        val translatedDataX = xValue.get - min
        //scale it to [0,1]
        val scaledDataX = translatedDataX/_range

        //compute all tile/bin coordinates (z, x, y, bX, bY)
        var howFarX = scaledDataX * tileCounts(z)
        var x = howFarX.toInt
        var xBin = ((howFarX - x)*maxBin).toInt
        Some(((z, x), xBin))
      }
    }
  }

  override def binTo1D(bin: Int, maxBin: Int): Int = {
    bin
  }
}
