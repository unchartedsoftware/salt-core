package com.unchartedsoftware.mosaic.core.projection

import org.scalatest._
import com.unchartedsoftware.mosaic.core.projection._
import org.apache.spark.sql.Row

class CartesianProjectionSpec extends FunSpec {
  describe("CartesianProjection") {
    describe("#getZoomLevel()") {
      it("should return the first component of a tile coordinate as the zoom level") {
        val projection = new CartesianProjection(100, 100, 0, 1, 0, 1D, 0D, 1, 1D, 0D)
        val coord = (Math.round(Math.random*100).toInt, 0, 0)
        assert(projection.getZoomLevel(coord) === coord._1)
      }
    }

    describe("#rowToCoords()") {
      it("should throw an exception for zoom levels outside of the bounds supplied as construction parameters") {
        val projection = new CartesianProjection(100, 100, 0, 1, 0, 1D, 0D, 1, 1D, 0D)
        val row = Row(Math.random, Math.random)
        intercept[Exception] {
          projection.rowToCoords(row, 2)
        }
      }

      it("should return None when a row's xCol is outside of the defined bounds") {
        val projection = new CartesianProjection(100, 100, 0, 1, 0, 1D, 0D, 1, 1D, 0D)
        assert(projection.rowToCoords(Row(projection.maxX+1, Math.random), 0) === None)
        assert(projection.rowToCoords(Row(projection.minX-1, Math.random), 0) === None)
      }

      it("should return None when a row's yCol is outside of the defined bounds") {
        val projection = new CartesianProjection(100, 100, 0, 1, 0, 1D, 0D, 1, 1D, 0D)
        assert(projection.rowToCoords(Row(Math.random, projection.maxY+1), 0) === None)
        assert(projection.rowToCoords(Row(Math.random, projection.minY-1), 0) === None)
      }

      it("should assign all Rows to the same tile at zoom level 0, to the correct bin") {
        val projection = new CartesianProjection(100, 100, 0, 1, 0, 1D, 0D, 1, 1D, 0D)
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
        val projection = new CartesianProjection(100, 100, 0, 1, 0, 1D, 0D, 1, 1D, 0D)
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
