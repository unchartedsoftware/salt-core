package com.unchartedsoftware.salt.core.analytic.numeric

import org.scalatest._
import com.unchartedsoftware.salt.core.generation.request._
import com.unchartedsoftware.salt.core.projection._
import org.apache.spark.sql.Row

class TileSeqRequestSpec extends FunSpec {
  describe("TileSeqRequest") {
    describe("#levels()") {
      it("should return the set of levels passed in to the request") {
        val tiles = Seq((0,0), (1,0), (1,1), (2,0), (5,0))
        val request = new TileSeqRequest(tiles, (t: (Int, Int)) => t._1)
        assert(request.levels.equals(Seq(0,1,2,5)))
        assert(!request.levels.equals(Seq(0,5,7)))
      }
    }

    describe("#inRequest()") {
      for (i <- 0 until 100) {
        val tiles = Seq(0,1,2,3,4,5).map(a => (a, (Math.random*Math.pow(2, a)).toInt))
        val request = new TileSeqRequest(tiles, (t: (Int, Int)) => t._1)
        tiles.foreach(a => {
          assert(request.inRequest(a))
        })
        assert(!request.inRequest((6, (Math.random*Math.pow(2, 6)).toInt)))
      }
    }
  }
}
