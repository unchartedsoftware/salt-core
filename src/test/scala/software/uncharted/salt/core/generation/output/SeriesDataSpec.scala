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

package software.uncharted.salt.core.generation.output

import org.scalatest._
import software.uncharted.salt.Spark
import software.uncharted.salt.core.util.SparseArray
import software.uncharted.salt.core.projection.numeric.SeriesProjection

class SeriesDataSpec extends FunSpec {
  describe("SeriesData") {
    val projection = new SeriesProjection(Seq(0), 0, 1)

    describe("#apply()") {
      it("should access the given bin using a bin coordinate, returning the appropriate value") {
        val bins = new SparseArray(2, 0, Map(1 -> 12))
        val data = new SeriesData(projection, 1, (0, 0), bins, None)
        assert(data(0) == bins(0))
        assert(data(1) == bins(1))
      }
    }

    describe("#merge()") {
      it("should allow clients to merge bins from two compatible SeriesData objects") {
        val bins = new SparseArray(2, 0, Map(1 -> 12))
        val data = new SeriesData(projection, 1, (0, 0), bins, Some("hello"))

        val otherBins = new SparseArray(2, 4, Map(1 -> 6))
        val otherData = new SeriesData(projection, 1, (0, 0), otherBins, Some("world"))

        val result = data.merge(otherData, (l: Int, r: Int) => l+r)

        assert(result(0) == bins(0) + otherBins(0))
        assert(result(1) == bins(1) + otherBins(1))
        assert(result.bins.default == bins.default + otherBins.default)
      }

      it("should allow clients to merge tile metadata from two compatible SeriesData objects") {
        val bins = new SparseArray(2, 0, Map(1 -> 12))
        val data = new SeriesData(projection, 1, (0, 0), bins, Some("hello"))

        val otherBins = new SparseArray(2, 4, Map(1 -> 6))
        val otherData = new SeriesData(projection, 1, (0, 0), otherBins, Some("world"))

        val result = data.merge(
          otherData,
          (l: Int, r: Int) => l+r,
          Some((l: String, r: String) => {
            l + " " + r
          })
        )

        assert(result(0) == bins(0) + otherBins(0))
        assert(result(1) == bins(1) + otherBins(1))
        assert(result.bins.default == bins.default + otherBins.default)
        assert(result.tileMeta.isDefined)
        assert(result.tileMeta.get.equals("hello world"))
      }

      it("should throw a SeriesDataMergeException if the two SeriesData objects have differing numbers of bins") {
        val bins = new SparseArray(2, 0, Map(1 -> 12))
        val data = new SeriesData(projection, 1, (0, 0), bins, None)

        val otherBins = new SparseArray(3, 4, Map(1 -> 6))
        val otherData = new SeriesData(projection, 2, (0, 0), otherBins, None)

        intercept[SeriesDataMergeException] {
          data.merge(otherData,(l: Int, r: Int) => l+r)
        }
      }

      it("should throw a SeriesDataMergeException if the two SeriesData objects have different tile coordinates") {
        val bins = new SparseArray(2, 0, Map(1 -> 12))
        val data = new SeriesData(projection, 1, (0, 0), bins, None)

        val otherBins = new SparseArray(2, 4, Map(1 -> 6))
        val otherData = new SeriesData(projection, 1, (1, 0), otherBins, None)

        intercept[SeriesDataMergeException] {
          data.merge(otherData,(l: Int, r: Int) => l+r)
        }
      }
    }
  }
}
