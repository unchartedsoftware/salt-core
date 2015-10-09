package software.uncharted.salt.core.analytic.numeric

import org.scalatest._
import software.uncharted.salt.core.generation.request._
import software.uncharted.salt.core.projection._
import org.apache.spark.sql.Row

class TileLevelRequestSpec extends FunSpec {
  describe("TileLevelRequest") {
    describe("#levels()") {
      it("should return the set of levels passed in to the request") {
        val levels = Seq(0,1,2,3,4,5)
        val request = new TileLevelRequest(levels, (t: (Int, Int)) => t._1)
        assert(request.levels.equals(levels))
        assert(!request.levels.equals(Seq(0,5,7)))
      }
    }

    describe("#inRequest()") {
      val request = new TileLevelRequest(Seq(0,1,2,3,4,5), (t: (Int, Int)) => t._1)
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
