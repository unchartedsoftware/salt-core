package com.unchartedsoftware.mosaic.core.projection.numeric

import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import org.apache.spark.sql.Row

class CartesianProjection(
  val xBins: Int,
  val yBins: Int,
  minZoom: Int,
  maxZoom: Int,
  source: ValueExtractor[(Double, Double)],
  min: (Double, Double),
  max: (Double, Double)) extends NumericProjection[(Int, Int, Int), (Double, Double)](xBins*yBins, minZoom, maxZoom, source, min, max) {

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

  override def rowToCoords (r: Row, z: Int): Option[((Int, Int, Int), Int)] = {
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
        var xBin = ((howFarX - x)*xBins).toInt
        var yBin = (yBins-1) - ((howFarY - y)*yBins).toInt
        Some(((z, x, y), xBin + yBin*xBins))
      }
    }
  }
}
