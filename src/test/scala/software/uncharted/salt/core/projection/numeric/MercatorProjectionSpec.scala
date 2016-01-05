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

class MercatorProjectionSpec extends FunSpec {
  describe("MercatorProjection") {
    describe("#project()") {
      it("should return None when the data-space coordinate is None") {
        val projection = new MercatorProjection((-180D, -85D), (180D, 85D), Seq(0))
        assert(projection.project(None, (100, 100)) === None)
      }

      it("should return None when a row's xCol is outside of the defined bounds") {
        val projection = new MercatorProjection((-180D, -85D), (180D, 85D), Seq(0))
        assert(projection.project(Some((projection.max._1 + 1, Math.random)), (100, 100)) === None)
        assert(projection.project(Some((projection.min._1 - 1, Math.random)), (100, 100)) === None)
        assert(projection.project(Some((projection.max._1, Math.random)), (100, 100)) === None)
        assert(projection.project(Some((projection.min._1, Math.random)), (100, 100)) === None)
      }

      it("should return None when a row's yCol is outside of the defined bounds") {
        val projection = new MercatorProjection((-180D, -85D), (180D, 85D), Seq(0))
        assert(projection.project(Some((Math.random, projection.max._2 + 1)), (100, 100)) === None)
        assert(projection.project(Some((Math.random, projection.min._2 - 1)), (100, 100)) === None)
        assert(projection.project(Some((Math.random, projection.max._2)), (100, 100)) === None)
        assert(projection.project(Some((Math.random, projection.min._2)), (100, 100)) === None)
      }

      it("should assign all Rows to the same tile at zoom level 0, to the correct bin") {
        val projection = new MercatorProjection((-180D, -85D), (180D, 85D), Seq(0))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some((Math.random*360-180, Math.random*170-85))
          val coords = projection.project(row, (99, 99))
          assert(coords.isDefined)

          //check that one coordinate is projected for each input zoom level
          assert(coords.get.length === 1)

          //check zoom level
          assert(coords.get(0)._1._1 === 0, "check zoom level")

          //compute coordinates and bin for lat/lon manually
          val lon = row.get._1
          val lat = row.get._2
          val latRad = (-lat) * (Math.PI / 180);
          val n = 1 << 0;
          val howFarX = n * ((lon + 180) / 360);
          val howFarY = n * (1-(Math.log(Math.tan(latRad) + 1/Math.cos(latRad)) / Math.PI)) / 2

          val x = howFarX.toInt
          val y = howFarY.toInt

          var xBin = ((howFarX - x)*100).toInt
          var yBin = (99) - ((howFarY - y)*100).toInt

          //check coordinates
          assert(coords.get(0)._1._2 === x, "check coordinates")
          assert(coords.get(0)._1._3 === y, "check coordinates")

          //check bin
          assert(coords.get(0)._2._1 === xBin, "check x bin index for " + row.toString)
          assert(coords.get(0)._2._2 === yBin, "check y bin index for " + row.toString)
          assert(coords.get(0)._2._1*coords.get(0)._2._2 < 100*100)
        }
      }

      it("should assign Rows to the correct tile and bin based on the given zoom level in TMS orientation") {
        val projection = new MercatorProjection((-180D, -85D), (180D, 85D), Seq(1))
        //fuzz inputs
        for (i <- 0 until 100) {
          val row = Some((Math.random*360-180, Math.random*170-85))
          val coords = projection.project(row, (99, 99))
          assert(coords.isDefined)

          //check that one coordinate is projected for each input zoom level
          assert(coords.get.length === 1)

          //check zoom level
          assert(coords.get(0)._1._1 === 1, "check zoom level")

          //compute coordinates and bin for lat/lon manually
          val lon = row.get._1
          val lat = row.get._2
          val latRad = (-lat) * (Math.PI / 180);
          val n = 1 << 1;
          val howFarX = n * ((lon + 180) / 360);
          val howFarY = n * (1-(Math.log(Math.tan(latRad) + 1/Math.cos(latRad)) / Math.PI)) / 2

          val x = howFarX.toInt
          val y = howFarY.toInt

          var xBin = ((howFarX - x)*100).toInt
          var yBin = (99) - ((howFarY - y)*100).toInt

          //check coordinates
          assert(coords.get(0)._1._2 === x, "check coordinates")
          assert(coords.get(0)._1._3 === y, "check coordinates")

          //check bin
          assert(coords.get(0)._2._1 === xBin, "check x bin index for " + row.toString)
          assert(coords.get(0)._2._2 === yBin, "check y bin index for " + row.toString)
          assert(coords.get(0)._2._1*coords.get(0)._2._2 < 100*100)
        }
      }

      it("should provide flipped y tile coordinates in non-TMS orientation") {
        val zoom = 10
        val tmsProjection = new MercatorProjection((-180D, -85D), (180D, 85D), Seq(zoom), true)
        val stdProjection = new MercatorProjection((-180D, -85D), (180D, 85D), Seq(zoom), false)

        val n = Math.pow(2, zoom).toInt - 1
        for (i <- -50D until 50D by 10D) {
          val row = Some((i,i))

          val tms = tmsProjection.project(row, (99,99))
          val std = stdProjection.project(row, (99,99))

          assert(tms.get(0)._1._1 === std.get(0)._1._1, "zoom level doesn't match between TMS and standard mercator projection")
          assert(tms.get(0)._1._2 === std.get(0)._1._2, "x-coord doesn't match between TMS and standard mercator projection")
          assert(tms.get(0)._1._3 === (n - std.get(0)._1._3), "y-coord of TMS is not flipped value of standard mercator projection")

          assert(tms.get(0)._2._1 === std.get(0)._2._1, "x-bin doesn't match between TMS and standard mercator projection")
          assert(tms.get(0)._2._2 === std.get(0)._2._2, "y-bin doesn't match between TMS and standard mercator projection")
        }
      }

      //TODO test multiple zoom levels
    }

    describe("#binTo1D()") {
      it("should convert a 2D bin coordinate into row-major order") {
        val projection = new MercatorProjection((-180D, -85D), (180D, 85D), Seq(0))

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
