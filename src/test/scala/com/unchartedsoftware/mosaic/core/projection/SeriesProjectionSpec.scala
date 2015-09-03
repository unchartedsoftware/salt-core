package com.unchartedsoftware.mosaic.core.projection

import org.scalatest._
import com.unchartedsoftware.mosaic.core.projection._
import org.apache.spark.sql.Row

class SeriesProjectionSpec extends FunSpec {
  describe("SeriesProjection") {
    describe("#getZoomLevel()") {
      it("should return the first component of a tile coordinate as the zoom level") {
        val projection = new SeriesProjection(100, 0, 1, 0, 100, 0)
        val coord = (Math.round(Math.random*100).toInt, 1)
        assert(projection.getZoomLevel(coord) === coord._1)
      }
    }

    describe("#rowToCoords()") {
      it("should throw an exception for zoom levels outside of the bounds supplied as construction parameters") {
        val projection = new SeriesProjection(100, 0, 1, 0, 100, 0)
        val row = Row(12D)
        intercept[Exception] {
          projection.rowToCoords(row, 2)
        }
      }

      it("should return None when a row's xCol is outside of the defined bounds") {
        val projection = new SeriesProjection(100, 0, 1, 0, 1D, 0D)
        assert(projection.rowToCoords(Row(projection.maxX+1), 0) === None)
        assert(projection.rowToCoords(Row(projection.minX-1), 0) === None)
      }

      it("should assign all Rows to the same tile at zoom level 0, to the correct bin") {
        val projection = new SeriesProjection(100, 0, 1, 0, 1D, 0D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Row(Math.random)
          val coords = projection.rowToCoords(row, 0)
          assert(coords.isDefined)
          assert(projection.getZoomLevel(coords.get._1) === 0)
          assert(coords.get._1._2 === 0)
          assert(coords.get._2 === Math.floor(row.getDouble(0)*100))
        }
      }

      it("should assign Rows to the corect tile and bin based on the given zoom level") {
        val projection = new SeriesProjection(100, 0, 1, 0, 1D, 0D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Row(Math.random)
          val coords = projection.rowToCoords(row, 1)
          assert(coords.isDefined)
          assert(projection.getZoomLevel(coords.get._1) === 1)
          assert(coords.get._1._2 === Math.floor(row.getDouble(0)*2))
          val bin = Math.floor( (row.getDouble(0)*200) % 100).toInt
          assert(coords.get._2 === bin)
        }
      }
    }
  }
}
