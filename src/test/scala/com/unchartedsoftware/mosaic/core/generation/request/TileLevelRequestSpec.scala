package com.unchartedsoftware.mosaic.core.analytic.numeric

import org.scalatest._
import com.unchartedsoftware.mosaic.core.generation.request._
import com.unchartedsoftware.mosaic.core.projection._
import org.apache.spark.sql.Row

class TileLevelRequestSpec extends FunSpec {
  val projection = new {

  } with Projection[(Int, Int), Int](0, 17) {
    override def getZoomLevel(c: (Int, Int)): Int = {
      c._1
    }
    override def rowToCoords(r: Row, l: Int, maxBin: Int): Option[((Int, Int), Int)] = {
      throw new UnsupportedOperationException
    }
    override def binTo1D(bin: Int, maxBin: Int): Int = {
      throw new UnsupportedOperationException
    }
  }

  describe("TileLevelRequest") {
    describe("#levels()") {
      it("should return the set of levels passed in to the request") {
        val levels = Seq(0,1,2,3,4,5)
        val request = new TileLevelRequest(levels, projection)
        assert(request.levels.equals(levels))
        assert(!request.levels.equals(Seq(0,5,7)))
      }
    }

    describe("#inRequest()") {
      val request = new TileLevelRequest(Seq(0,1,2,3,4,5), projection)
      for (i <- 0 until 100) {
        val level = (Math.random*10).toInt
        if (level < 6) {
          assert(request.inRequest((level, 0)))
        } else {
          assert(!request.inRequest((level, 0)))
        }
      }
    }
  }
}
