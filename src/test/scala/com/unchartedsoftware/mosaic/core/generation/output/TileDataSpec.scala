package com.unchartedsoftware.mosaic.core.analytic.numeric

import org.scalatest._
import com.unchartedsoftware.mosaic.core.generation.output._
import com.unchartedsoftware.mosaic.core.projection._
import org.apache.spark.sql.Row

class TileDataSpec extends FunSpec {
  val projection = new {

  } with Projection[(Int, Int), Int](0, 17) {
    override def getZoomLevel(c: (Int, Int)): Int = {
      throw new UnsupportedOperationException
    }
    override def rowToCoords(r: Row, l: Int, maxBin: Int): Option[((Int, Int), Int)] = {
      throw new UnsupportedOperationException
    }
    override def binTo1D(bin: Int, maxBin: Int): Int = {
      throw new UnsupportedOperationException
    }
  }


  describe("TileData") {
    describe("#getBin()") {
      it("should return the correct bin based on an index") {
        val bins = Seq(0,1,2,3)
        val data = new TileData[(Int, Int), Int, Int]((0, 0), bins, 3, 2, 0, projection)
        for (i <- 0 until bins.length) {
          assert(data.getBin(i).equals(bins(i)))
        }
      }
    }
  }
}
