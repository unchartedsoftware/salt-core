/*
 * Copyright 2015 Uncharted Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package software.uncharted.salt.core.analytic.numeric

import org.scalatest._
import software.uncharted.salt.core.generation.output._
import software.uncharted.salt.core.projection._
import org.apache.spark.sql.Row

class TileDataSpec extends FunSpec {
  val projection = new {

  } with Projection[Double, (Int, Int), Int]() {
    override def project(dc: Option[Double], l: Int, maxBin: Int): Option[((Int, Int), Int)] = {
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
        val data = new TileData[(Int, Int), Int, Int]((0, 0), bins, 3, 2, Some(0), projection)
        for (i <- 0 until bins.length) {
          assert(data.getBin(i).equals(bins(i)))
        }
      }
    }
  }
}
