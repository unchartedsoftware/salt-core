package com.unchartedsoftware.mosaic.core.projection.numeric

import org.scalatest._
import com.unchartedsoftware.mosaic.core.projection._
import org.apache.spark.sql.Row

class CartesianProjectionSpec extends FunSpec {
  describe("CartesianProjection") {
    describe("#getZoomLevel()") {
      it("should return the first component of a tile coordinate as the zoom level") {
        val projection = new CartesianProjection(0, 1, (0D, 0D), (1D, 1D))
        val coord = (Math.round(Math.random*100).toInt, 0, 0)
        assert(projection.getZoomLevel(coord) === coord._1)
      }
    }

    describe("#project()") {
      it("should throw an exception for zoom levels outside of the bounds supplied as construction parameters") {
        val projection = new CartesianProjection(0, 1, (0D, 0D), (1D, 1D))
        intercept[Exception] {
          projection.project(Some((Math.random, Math.random)), 2, (100, 100))
        }
      }

      it("should return None when the data-space coordinate is None") {
        val projection = new CartesianProjection(0, 1, (0D, 0D), (1D, 1D))
        assert(projection.project(None, 0, (100, 100)) === None)
      }

      it("should return None when a row's xCol is outside of the defined bounds") {
        val projection = new CartesianProjection(0, 1, (0D, 0D), (1D, 1D))
        assert(projection.project(Some((projection.max._1+1, Math.random)), 0, (100, 100)) === None)
        assert(projection.project(Some((projection.min._1-1, Math.random)), 0, (100, 100)) === None)
        assert(projection.project(Some((projection.max._1, Math.random)), 0, (100, 100)) === None)
        assert(projection.project(Some((projection.min._1, Math.random)), 0, (100, 100)) === None)
      }

      it("should return None when a row's yCol is outside of the defined bounds") {
        val projection = new CartesianProjection(0, 1, (0D, 0D), (1D, 1D))
        assert(projection.project(Some((Math.random, projection.max._2+1)), 0, (100, 100)) === None)
        assert(projection.project(Some((Math.random, projection.min._2-1)), 0, (100, 100)) === None)
        assert(projection.project(Some((Math.random, projection.max._2)), 0, (100, 100)) === None)
        assert(projection.project(Some((Math.random, projection.min._2)), 0, (100, 100)) === None)
      }

      it("should assign all Rows to the same tile at zoom level 0, to the correct bin") {
        val projection = new CartesianProjection(0, 1, (0D, 0D), (1D, 1D))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some((Math.random, Math.random))
          val coords = projection.project(row, 0, (100, 100))
          assert(coords.isDefined)

          //check zoom level
          assert(projection.getZoomLevel(coords.get._1) === 0, "check zoom level")

          //check coordinates
          assert(coords.get._1._2 === 0, "check coordinates")
          assert(coords.get._1._3 === 0, "check coordinates")

          //check bin
          val xBin = (row.get._1*100).toInt;
          val yBin = (100-1) - (row.get._2*100).toInt;
          assert(coords.get._2._1 === xBin, "check x bin index for " + row.toString)
          assert(coords.get._2._2 === yBin, "check y bin index for " + row.toString)
        }
      }

      it("should assign Rows to the correct tile and bin based on the given zoom level") {
        val projection = new CartesianProjection(0, 1, (0D, 0D), (1D, 1D))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some((Math.random, Math.random))
          val coords = projection.project(row, 1, (100, 100))
          assert(coords.isDefined)

          //check zoom level
          assert(projection.getZoomLevel(coords.get._1) === 1)

          //check coordinates
          assert(coords.get._1._2 === Math.floor(row.get._1*2))
          assert(coords.get._1._3 === Math.floor(row.get._2*2))

          //check bin
          val xBin = ((row.get._1*200) % 100).toInt
          val yBin = (100 - 1) - ((row.get._2*200) % 100).toInt
          assert(coords.get._2._1 === xBin, "check x bin index for " + row.toString)
          assert(coords.get._2._2 === yBin, "check y bin index for " + row.toString)
        }
      }
    }

    describe("#binTo1D()") {
      it("should convert a 2D bin coordinate into row-major order") {
        val projection = new CartesianProjection(0, 1, (0D, 0D), (1D, 1D))

        //fuzz inputs
        for (i <- 0 until 100) {
          val bin = (Math.round(Math.random*99).toInt, Math.round(Math.random*99).toInt)
          assert(projection.binTo1D(bin, (100,100)) === bin._1 + bin._2*100)
        }
      }
    }
  }
}
