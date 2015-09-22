package com.unchartedsoftware.mosaic.core.projection

import org.scalatest._
import com.unchartedsoftware.mosaic.core.projection._
import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import org.apache.spark.sql.Row

class CartesianProjectionSpec extends FunSpec {
  describe("CartesianProjection") {
    describe("#getZoomLevel()") {
      it("should return the first component of a tile coordinate as the zoom level") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new CartesianProjection(100, 100, 0, 1, extractor, (0D, 0D), (1D, 1D))
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
        val projection = new CartesianProjection(100, 100, 0, 1, extractor, (0D, 0D), (1D, 1D))
        val row = Row(Math.random, Math.random)
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
        val projection = new CartesianProjection(100, 100, 0, 1, extractor, (0D, 0D), (1D, 1D))
        assert(projection.rowToCoords(Row(Math.random, Math.random), 0) === None)
      }

      it("should return None when a row's xCol is outside of the defined bounds") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new CartesianProjection(100, 100, 0, 1, extractor, (0D, 0D), (1D, 1D))
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
        val projection = new CartesianProjection(100, 100, 0, 1, extractor, (0D, 0D), (1D, 1D))
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
        val projection = new CartesianProjection(100, 100, 0, 1, extractor, (0D, 0D), (1D, 1D))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Row(Math.random, Math.random)
          val coords = projection.rowToCoords(row, 0)
          assert(coords.isDefined)

          //check zoom level
          assert(projection.getZoomLevel(coords.get._1) === 0, "check zoom level")

          //check coordinates
          assert(coords.get._1._2 === 0, "check coordinates")
          assert(coords.get._1._3 === 0, "check coordinates")

          //check bin
          val xBin = (row.getDouble(0)*projection.xBins).toInt;
          val yBin = (projection.yBins-1) - (row.getDouble(1)*projection.yBins).toInt;
          assert(coords.get._2 === (xBin + yBin*projection.xBins), "check bin index for " + row.toString)
        }
      }

      it("should assign Rows to the corect tile and bin based on the given zoom level") {
        val extractor = new ValueExtractor[(Double, Double)] {
          override def rowToValue(r: Row): Option[(Double, Double)] = {
            return Some((r.getDouble(0), r.getDouble(1)))
          }
        }
        val projection = new CartesianProjection(100, 100, 0, 1, extractor, (0D, 0D), (1D, 1D))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Row(Math.random, Math.random)
          val coords = projection.rowToCoords(row, 1)
          assert(coords.isDefined)

          //check zoom level
          assert(projection.getZoomLevel(coords.get._1) === 1)

          //check coordinates
          assert(coords.get._1._2 === Math.floor(row.getDouble(0)*2))
          assert(coords.get._1._3 === Math.floor(row.getDouble(1)*2))

          //check bin
          val xBin = ((row.getDouble(0)*200) % 100).toInt
          val yBin = (projection.yBins - 1) - ((row.getDouble(1)*200) % 100).toInt
          assert(coords.get._2 === (xBin + yBin*projection.xBins), "check bin index for " + row.toString)
        }
      }
    }
  }
}
