package com.unchartedsoftware.mosaic.core.projection

import com.unchartedsoftware.mosaic.util.DataFrameUtil
import org.apache.spark.sql.Row

class SeriesProjection(
  bins: Int,
  minZoom: Int,
  maxZoom: Int,
  val xCol: Int,
  val maxX: Double,
  val minX: Double) extends Projection[(Int, Int)](bins, minZoom, maxZoom) {

  //Precompute some stuff we'll use frequently
  val _range = maxX - minX
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

  override def rowToCoords (r: Row, z: Int): Option[((Int, Int), Int)] = {
    if (z > maxZoom || z < minZoom) {
      throw new Exception("Requested zoom level is outside this projection's zoom bounds.")
    } else {
      //convert value to a double
      val doubleX = DataFrameUtil.getDouble(xCol, r)

      if (doubleX > maxX || doubleX < minX) {
        None
      } else {
        val translatedDataX = doubleX - minX
        //scale it to [0,1]
        val scaledDataX = translatedDataX/_range

        //compute all tile/bin coordinates (z, x, y, bX, bY)
        var howFarX = scaledDataX * tileCounts(z)
        var x = howFarX.toInt
        var xBin = ((howFarX - x)*bins).toInt
        Some(((z, x), xBin))
      }
    }
  }
}
