/*
 * Copyright 2016 Uncharted Software Inc.
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

package software.uncharted.salt.core.projection.numeric

import org.scalatest._
import software.uncharted.salt.core.projection._
import org.apache.spark.sql.Row

class SeriesProjectionSpec extends FunSpec {
  describe("SeriesProjection") {
    describe("#project()") {
      it("should return None when the data-space coordinate is None") {
        val projection = new SeriesProjection(Seq(0), 0, 1)
        assert(projection.project(None, 100) === None)
      }

      it("should return None when a row's xCol is outside of the defined bounds") {
        val projection = new SeriesProjection(Seq(0), 0D, 1D)
        assert(projection.project(Some(projection.max + 1), 100) === None)
        assert(projection.project(Some(projection.min - 1), 100) === None)
        assert(projection.project(Some(projection.max), 100) === None)
        assert(projection.project(Some(projection.min), 100) === None)
      }

      it("should assign all Rows to the same tile at zoom level 0, to the correct bin") {
        val projection = new SeriesProjection(Seq(0), 0D, 1D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some(Math.random)
          val coords = projection.project(row, 99)
          assert(coords.isDefined)

          //check that one coordinate is projected for each input zoom level
          assert(coords.get.length === 1)

          //check zoom level
          assert(coords.get(0)._1._1 === 0, "check zoom level")

          //check coordinates
          assert(coords.get(0)._1._2 === 0, "check coordinates")

          //check bin
          assert(coords.get(0)._2 === Math.floor(row.get*100), "check bin index")
        }
      }

      it("should assign Rows to the corect tile and bin based on the given zoom level") {
        val projection = new SeriesProjection(Seq(1), 0D, 1D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some(Math.random)
          val coords = projection.project(row, 99)
          assert(coords.isDefined)

          //check that one coordinate is projected for each input zoom level
          assert(coords.get.length === 1)

          //check zoom level
          assert(coords.get(0)._1._1 === 1, "check zoom level")

          //check coordinates
          assert(coords.get(0)._1._2 === Math.floor(row.get*2), "check coordinates")

          //check bin
          val bin = ( (row.get*200) % 100).toInt
          assert(coords.get(0)._2 === bin, "check bin index")
        }
      }

      //TODO test multiple zoom levels
    }

    describe("#binTo1D()") {
      it("should be a no-op, returning the xBin passed in") {
        val projection = new SeriesProjection(Seq(0), 0D, 1D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val bin = Math.round(Math.random*99).toInt
          assert(projection.binTo1D(bin, 99) === bin)
        }
      }
    }

    describe("#binFrom1D()") {
      it("should be a no-op, returning the index passed in") {
        val projection = new SeriesProjection(Seq(0), 0D, 1D)
        //fuzz inputs
        for (i <- 0 until 100) {
          val bin = Math.round(Math.random*99).toInt
          assert(projection.binFrom1D(bin, 99) === bin)
        }
      }
    }
  }
}
