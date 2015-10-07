package com.unchartedsoftware.mosaic.core.projection.numeric

import org.scalatest._
import com.unchartedsoftware.mosaic.core.projection._
import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import org.apache.spark.sql.Row

class SeriesProjectionSpec extends FunSpec {
  describe("SeriesProjection") {
    describe("#getZoomLevel()") {
      it("should return the first component of a tile coordinate as the zoom level") {
        val projection = new SeriesProjection(0, 1, 0, 100)
        val coord = (Math.round(Math.random*100).toInt, 0)
        assert(projection.getZoomLevel(coord) === coord._1)
      }
    }

    describe("#project()") {
      it("should throw an exception for zoom levels outside of the bounds supplied as construction parameters") {
        val projection = new SeriesProjection(0, 1, 0, 100)
        intercept[Exception] {
          projection.project(Some(12D), 2, 100)
        }
      }

      it("should return None when the data-space coordinate is None") {
        val projection = new SeriesProjection(0, 1, 0, 1)
        assert(projection.project(None, 0, 100) === None)
      }

      it("should return None when a row's xCol is outside of the defined bounds") {
        val projection = new SeriesProjection(0, 1, 0D, 1D)
        assert(projection.project(Some(projection.max+1), 0, 100) === None)
        assert(projection.project(Some(projection.min-1), 0, 100) === None)
        assert(projection.project(Some(projection.max), 0, 100) === None)
        assert(projection.project(Some(projection.min), 0, 100) === None)
      }

      it("should assign all Rows to the same tile at zoom level 0, to the correct bin") {
        val projection = new SeriesProjection(0, 1, 0D, 1D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some(Math.random)
          val coords = projection.project(row, 0, 100)
          assert(coords.isDefined)

          //check zoom level
          assert(projection.getZoomLevel(coords.get._1) === 0, "check zoom level")

          //check coordinates
          assert(coords.get._1._2 === 0, "check coordinates")

          //check bin
          assert(coords.get._2 === Math.floor(row.get*100), "check bin index")
        }
      }

      it("should assign Rows to the corect tile and bin based on the given zoom level") {
        val projection = new SeriesProjection(0, 1, 0D, 1D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some(Math.random)
          val coords = projection.project(row, 1, 100)
          assert(coords.isDefined)

          //check zoom level
          assert(projection.getZoomLevel(coords.get._1) === 1, "check zoom level")

          //check coordinates
          assert(coords.get._1._2 === Math.floor(row.get*2), "check coordinates")

          //check bin
          val bin = ( (row.get*200) % 100).toInt
          assert(coords.get._2 === bin, "check bin index")
        }
      }
    }

    describe("#binTo1D()") {
      it("should be a no-op, returning the xBin passed in") {
        val projection = new SeriesProjection(0, 1, 0D, 1D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val bin = Math.round(Math.random*99).toInt
          assert(projection.binTo1D(bin, 100) === bin)
        }
      }
    }
  }
}
