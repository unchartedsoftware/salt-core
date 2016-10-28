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
      it("should return the element at the given index if materialized") {
        val test = SparseArray(2, -1, 0.0f)(0 -> 12)
        assert(test.isMaterialized)
        assert(test(0) === 12)
      }
      it("should return the default element if there is no value set at the given index if materialized") {
        val test = SparseArray(2, -1, 0.0f)(0 -> 12)
        assert(test.isMaterialized)
        assert(test(1) === -1)
      }
      it("should throw an ArrayOutOfBoundsException if the given index is outside the SparseArray size if materialized") {
        val test = SparseArray(2, -1, 0.0f)(0 -> 12)
        assert(test.isMaterialized)
        intercept[ArrayIndexOutOfBoundsException] {
          test(2)
        }
      }
      it("should return the element at the given index if not materialized") {
        val test = SparseArray(2, -1, 1.0f)(0 -> 12)
        assert(!test.isMaterialized)
        assert(test(0) === 12)
      }
      it("should return the default element if there is no value set at the given index if not materialized") {
        val test = SparseArray(2, -1, 1.0f)(0 -> 12)
        assert(!test.isMaterialized)
        assert(test(1) === -1)
      }
      it("should throw an ArrayOutOfBoundsException if the given index is outside the SparseArray size if not materialized") {
        val test = SparseArray(2, -1, 1.0f)(0 -> 12)
        assert(!test.isMaterialized)
        intercept[ArrayIndexOutOfBoundsException] {
          test(2)
        }
      }
    }

    describe("#length()") {
      it("should return the size of the SparseArray") {
        val test = SparseArray(2, -1)()
        assert(test.length() === 2)
      }
    }

    describe("#density()") {
      it("should return the density of the SparseArray") {
        val test = SparseArray(2, -1, 1.0f)()
        assert(test.length() === 2)
        assert(test.density() === 0.0f)
        test(0) = -1
        assert(test.density() === 0.0f)
        test(1) = 2
        assert(test.density() === 0.5f)
      }

      it("should not cause materialization even at maximum density when the materialization threshold is set to 1.0") {
        val test = SparseArray(2, -1, 1.0f)(0 -> 3, 1 -> 2)
        assert(!test.isMaterialized)
      }
    }

    describe("#update()") {
      it("should set the element at the given index if the given index never had a value") {
        val test = SparseArray(2, -1)()
        test(0) = 12
        assert(test(0) === 12)
      }

      it("should set the element at the given index if the given index was already set") {
        val test = SparseArray(2, -1)(0 -> 0)
        test(0) = 12
        assert(test(0) === 12)
      }

      it("should safely set the element at the given index to the default value, eliminating an existing stored value if one was present") {
        val test = SparseArray(5, -1)(0 -> 0)
        test(0) = 12
        assert(test(0) === 12)
        test(0) = -1
        test(1) = -1
        assert(test(0) === -1)
        assert(test(1) === -1)
        assert(test.density() === 0.0f)
      }

      it("should throw an ArrayOutOfBoundsException if the given index is outside the SparseArray size") {
        val test = SparseArray(2, -1)()
        intercept[ArrayIndexOutOfBoundsException] {
          test.update(2, 12)
        }
      }
    }

    describe("#seq()") {
      it("should convert the SparseArray to an IndexedSeq") {
        val test = SparseArray(2, -1)(0 -> 0)
        val seq = test.seq
        assert(seq.isInstanceOf[scala.collection.IndexedSeq[Int]])
        assert(seq.length === test.length)
      }
    }

    describe("Builder") {
      val test = SparseArray(2, -1)(0 -> 0)
//      describe("#newBuilder()") {
//        it("should return a new, empty SparseArray with the same default value") {
//          val b = test.newBuilder()
//          assert(b.result.size == 0)
//          assert(b.result.default == test.default)
//        }
//      }
//      describe("#+=()") {
//        it("should append an element to the SparseArray builder") {
//          val b = test.newBuilder()
//          b += 12
//          assert(b.result.size == 1)
//          assert(b.result()(0) == 12)
//        }
//        it("should safely append a default element to the SparseArray builder") {
//          val b = test.newBuilder()
//          b += -1
//          assert(b.result.size == 1)
//          assert(b.result()(0) == -1)
//        }
//      }
//      describe("#clear()") {
//        it("should clear the SparseArray builder") {
//          val b = test.newBuilder()
//          b += 12
//          b.clear()
//          assert(b.result.size == 0)
//        }
//      }
//      describe("#result()") {
//        it("should convert the SparseArray builder into a SparseArray") {
//          val b = test.newBuilder()
//          b += 12
//          assert(b.result().isInstanceOf[SparseArray[Int]])
//          assert(b.result().length() == 1)
//          assert(b.result()(0) == 12)
//        }
//      }
    }
  }
}
