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

package software.uncharted.salt.core.projection.numeric

import org.scalatest._
import software.uncharted.salt.core.projection._
import org.apache.spark.sql.Row

class CartesianProjectionSpec extends FunSpec {
  describe("CartesianProjection") {
    describe("#project()") {
      it("should return None when the data-space coordinate is None") {
        val projection = new CartesianProjection(Seq(0), (0D, 0D), (1D, 1D))
        assert(projection.project(None, (100, 100)) === None)
      }

      // epsilon value for bounds checks
      val epsilon = 1E-12
      // A random guaranteed to be in bounds, for bounds checks
      def boundedRandom = Math.random * (1.0 - 2.0 * epsilon) + epsilon

      it("should return None when a row's xCol is outside of the defined bounds") {
        val projection = new CartesianProjection(Seq(0), (0D, 0D), (1D, 1D))
        assert(projection.project(Some((projection.max._1 + epsilon, boundedRandom)), (100, 100)) === None)
        assert(projection.project(Some((projection.min._1 - epsilon, boundedRandom)), (100, 100)) === None)
        assert(projection.project(Some((projection.max._1, boundedRandom)), (100, 100)) === None)
        assert(projection.project(Some((projection.min._1, boundedRandom)), (100, 100)).isDefined)
        assert(projection.project(Some((projection.max._1 - epsilon, boundedRandom)), (100, 100)).isDefined)
        assert(projection.project(Some((projection.min._1 + epsilon, boundedRandom)), (100, 100)).isDefined)
      }

      it("should return None when a row's yCol is outside of the defined bounds") {
        val projection = new CartesianProjection(Seq(0), (0D, 0D), (1D, 1D))
        assert(projection.project(Some((boundedRandom, projection.max._2 + epsilon)), (100, 100)) === None)
        assert(projection.project(Some((boundedRandom, projection.min._2 - epsilon)), (100, 100)) === None)
        assert(projection.project(Some((boundedRandom, projection.max._2)), (100, 100)) === None)
        assert(projection.project(Some((boundedRandom, projection.min._2)), (100, 100)).isDefined)
        assert(projection.project(Some((boundedRandom, projection.max._2 - epsilon)), (100, 100)).isDefined)
        assert(projection.project(Some((boundedRandom, projection.min._2 + epsilon)), (100, 100)).isDefined)
      }

      // Note from here down, Math.random will do unadorned, as it is closed-open, just like bins are
      it("should assign all Rows to the same tile at zoom level 0, to the correct bin") {
        val projection = new CartesianProjection(Seq(0), (0D, 0D), (1D, 1D))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some((Math.random, Math.random))
          val coords = projection.project(row, (99, 99))
          assert(coords.isDefined)

          //check that one coordinate is projected for each input zoom level
          assert(coords.get.length === 1)

          //check zoom level
          assert(coords.get(0)._1._1 === 0, "check zoom level")

          //check coordinates
          assert(coords.get(0)._1._2 === 0, "check coordinates")
          assert(coords.get(0)._1._3 === 0, "check coordinates")

          //check bin
          val xBin = (row.get._1*100).toInt;
          val yBin = (99) - (row.get._2*100).toInt;
          assert(coords.get(0)._2._1 === xBin, "check x bin index for " + row.toString)
          assert(coords.get(0)._2._2 === yBin, "check y bin index for " + row.toString)
          assert(coords.get(0)._2._1*coords.get(0)._2._2 < 100*100)
        }
      }

      it("should assign Rows to the correct tile and bin based on the given zoom level") {
        val projection = new CartesianProjection(Seq(1), (0D, 0D), (1D, 1D))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some((Math.random, Math.random))
          val coords = projection.project(row, (99, 99))
          assert(coords.isDefined)

          //check that one coordinate is projected for each input zoom level
          assert(coords.get.length === 1)

          //check zoom level
          assert(coords.get(0)._1._1 === 1)

          //check coordinates
          assert(coords.get(0)._1._2 === Math.floor(row.get._1*2))
          assert(coords.get(0)._1._3 === Math.floor(row.get._2*2))

          //check bin
          val xBin = ((row.get._1*200) % 100).toInt
          val yBin = (99) - ((row.get._2*200) % 100).toInt
          assert(coords.get(0)._2._1 === xBin, "check x bin index for " + row.toString)
          assert(coords.get(0)._2._2 === yBin, "check y bin index for " + row.toString)
          assert(coords.get(0)._2._1*coords.get(0)._2._2 < 100*100)
        }
      }

      //TODO test multiple zoom levels
    }

    describe("#binTo1D()") {
      it("should convert a 2D bin coordinate into row-major order") {
        val projection = new CartesianProjection(Seq(0), (0D, 0D), (1D, 1D))

        //fuzz inputs
        for (i <- 0 until 100) {
          val bin = (Math.round(Math.random*99).toInt, Math.round(Math.random*99).toInt)
          val unidim = projection.binTo1D(bin, (99,99))
          assert(unidim <= 100*100)
          assert(unidim === bin._1 + bin._2*100)
        }
      }
    }
  }
}
