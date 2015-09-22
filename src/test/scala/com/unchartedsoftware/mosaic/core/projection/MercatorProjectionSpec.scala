package com.unchartedsoftware.mosaic.core.projection

import org.scalatest._
import com.unchartedsoftware.mosaic.core.projection._
import com.unchartedsoftware.mosaic.util._
import com.unchartedsoftware.mosaic.core.analytic.ValueExtractor
import org.apache.spark.sql.Row

class MercatorProjectionSpec extends FunSpec {
  describe("MercatorProjection") {
    describe("#getZoomLevel()") {
      it("should return the first component of a tile coordinate as the zoom level") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new MercatorProjection(100, 100, 0, 1, extractor, (-180D, -85D), (180D, 85D))
        val coord = (Math.round(Math.random*100).toInt, 0, 0)
        assert(projection.getZoomLevel(coord) === coord._1)
      }
    }

    describe("#rowToCoords()") {
      it("should throw an exception for zoom levels outside of the bounds supplied as construction parameters") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new MercatorProjection(100, 100, 0, 1, extractor, (-180D, -85D), (180D, 85D))
        val row = Row(Math.random*170-85, Math.random*360-180)
        intercept[Exception] {
          projection.rowToCoords(row, 2)
        }
      }

      it("should return None when its data-space value extractor returns None") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return None
          }
        }
        val projection = new MercatorProjection(100, 100, 0, 1, extractor, (-180D, -85D), (180D, 85D))
        val row = Row(Math.random*170-85, Math.random*360-180)
        assert(projection.rowToCoords(row, 0) === None)
      }

      it("should return None when a row's xCol is outside of the defined bounds") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new MercatorProjection(100, 100, 0, 1, extractor, (-180D, -85D), (180D, 85D))
        assert(projection.rowToCoords(Row(projection.max._1+1, Math.random), 0) === None)
        assert(projection.rowToCoords(Row(projection.min._1-1, Math.random), 0) === None)
        assert(projection.rowToCoords(Row(projection.max._1, Math.random), 0) === None)
        assert(projection.rowToCoords(Row(projection.min._1, Math.random), 0) === None)
      }

      it("should return None when a row's yCol is outside of the defined bounds") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new MercatorProjection(100, 100, 0, 1, extractor, (-180D, -85D), (180D, 85D))
        assert(projection.rowToCoords(Row(Math.random, projection.max._2+1), 0) === None)
        assert(projection.rowToCoords(Row(Math.random, projection.min._2-1), 0) === None)
        assert(projection.rowToCoords(Row(Math.random, projection.max._2), 0) === None)
        assert(projection.rowToCoords(Row(Math.random, projection.min._2), 0) === None)
      }

      it("should assign all Rows to the same tile at zoom level 0, to the correct bin") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new MercatorProjection(100, 100, 0, 1, extractor, (-180D, -85D), (180D, 85D))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Row(Math.random*360-180, Math.random*170-85)
          val coords = projection.rowToCoords(row, 0)
          assert(coords.isDefined)

          //check zoom level
          assert(projection.getZoomLevel(coords.get._1) === 0, "check zoom level")

          //compute coordinates and bin for lat/lon manually
          val lon = DataFrameUtil.getDouble(0, row)
          val lat = DataFrameUtil.getDouble(1, row)
          val latRad = (-lat) * (Math.PI / 180);
          val n = 1 << 0;
          val howFarX = n * ((lon + 180) / 360);
          val howFarY = n * (1-(Math.log(Math.tan(latRad) + 1/Math.cos(latRad)) / Math.PI)) / 2

          val x = howFarX.toInt
          val y = howFarY.toInt

          var xBin = ((howFarX - x)*projection.xBins).toInt
          var yBin = (projection.yBins-1) - ((howFarY - y)*projection.yBins).toInt

          //check coordinates
          assert(coords.get._1._2 === x, "check coordinates")
          assert(coords.get._1._3 === y, "check coordinates")

          //check bin
          assert(coords.get._2 === (xBin + yBin*projection.xBins), "check bin index for " + row.toString)
        }
      }

      it("should assign Rows to the corect tile and bin based on the given zoom level") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new MercatorProjection(100, 100, 0, 1, extractor, (-180D, -85D), (180D, 85D))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Row(Math.random*360-180, Math.random*170-85)
          val coords = projection.rowToCoords(row, 1)
          assert(coords.isDefined)

          //check zoom level
          assert(projection.getZoomLevel(coords.get._1) === 1, "check zoom level")

          //compute coordinates and bin for lat/lon manually
          val lon = DataFrameUtil.getDouble(0, row)
          val lat = DataFrameUtil.getDouble(1, row)
          val latRad = (-lat) * (Math.PI / 180);
          val n = 1 << 1;
          val howFarX = n * ((lon + 180) / 360);
          val howFarY = n * (1-(Math.log(Math.tan(latRad) + 1/Math.cos(latRad)) / Math.PI)) / 2

          val x = howFarX.toInt
          val y = howFarY.toInt

          var xBin = ((howFarX - x)*projection.xBins).toInt
          var yBin = (projection.yBins-1) - ((howFarY - y)*projection.yBins).toInt

          //check coordinates
          assert(coords.get._1._2 === x, "check coordinates")
          assert(coords.get._1._3 === y, "check coordinates")

          //check bin
          assert(coords.get._2 === (xBin + yBin*projection.xBins), "check bin index for " + row.toString)
        }
      }
    }
  }
}
