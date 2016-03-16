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

package software.uncharted.salt.core.util

import org.scalatest._

class SparseArraySpec extends FunSpec {
  describe("SparseArray") {
    describe("#apply()") {
      it("should return the element at the given index") {
        val test = new SparseArray(2, -1, Map(0 -> 12))
        assert(test(0) == 12)
      }
      it("should return the default element if there is no value set at the given index") {
        val test = new SparseArray(2, -1, Map(0 -> 12))
        assert(test(1) == -1)
      }
      it("should throw an ArrayOutOfBoundsException if the given index is outside the SparseArray size") {
        val test = new SparseArray(2, -1, Map())
        intercept[ArrayIndexOutOfBoundsException] {
          test(2)
        }
      }
    }

    describe("#length()") {
      it("should return the size of the SparseArray") {
        val test = new SparseArray(2, -1, Map())
        assert(test.length() == 2)
      }
    }

    describe("#update()") {
      it("should set the element at the given index if the given index never had a value") {
        val test = new SparseArray(2, -1, Map())
        test.update(0, 12)
        assert(test(0) == 12)
      }

      it("should set the element at the given index if the given index was already set") {
        val test = new SparseArray(2, -1, Map(0 -> 0))
        test.update(0, 12)
        assert(test(0) == 12)
      }

      it("should throw an ArrayOutOfBoundsException if the given index is outside the SparseArray size") {
        val test = new SparseArray(2, -1, Map())
        intercept[ArrayIndexOutOfBoundsException] {
          test.update(2, 12)
        }
      }
    }

    describe("#seq()") {
      it("should convert the SparseArray to an IndexedSeq") {
        val test = new SparseArray(2, -1, Map(0 -> 0))
        val seq = test.seq
        assert(seq.isInstanceOf[scala.collection.IndexedSeq[Int]])
        assert(seq.length == test.length)
      }
    }

    describe("Builder") {
      val test = new SparseArray(2, -1, Map(0 -> 0))
      describe("#newBuilder()") {
        it("should return a new, empty SparseArray with the same default value") {
          val b = test.newBuilder()
          assert(b.result.size == 0)
          assert(b.result.default == test.default)
        }
      }
      describe("#+=()") {
        it("should append an element to the SparseArray builder") {
          val b = test.newBuilder()
          b += 12
          assert(b.result.size == 1)
          assert(b.result()(0) == 12)
        }
      }
      describe("#clear()") {
        it("should clear the SparseArray builder") {
          val b = test.newBuilder()
          b += 12
          b.clear()
          assert(b.result.size == 0)
        }
      }
      describe("#result()") {
        it("should convert the SparseArray builder into a SparseArray") {
          val b = test.newBuilder()
          b += 12
          assert(b.result().isInstanceOf[SparseArray[Int]])
          assert(b.result().length() == 1)
          assert(b.result()(0) == 12)
        }
      }
    }
  }
}
