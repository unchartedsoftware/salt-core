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

import scala.collection.mutable.Buffer
import org.scalatest._

class SparseArraySpec extends FunSpec {
  describe("SparseArray") {
    it("Should be serializable") {
      assert(classOf[Serializable].isAssignableFrom(classOf[SparseArray[_]]))
    }

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

    describe("#merge") {
      def add (p: SparseArray[Int], q: SparseArray[Int]): SparseArray[Int] =
        SparseArray.merge((pp: Int, qq: Int) => pp + qq)(p, q)

      it("should merge two sparse arrays without creating extra elements") {
        val a = SparseArray(3, 0, 1.0f)(0 -> 2)
        val b = SparseArray(3, 0, 1.0f)(1 -> 4)
        val c = SparseArray.merge((aa: Int, bb: Int) => 3*aa+5*bb)(a, b)
        assert(c(0) === 6)
        assert(c(1) === 20)
        assert(c(2) === 0)
        assert(c.length() === 3)
        assert(c.density() === 2/3f)
      }

      it("should result in a sparse array as long as the longer input") {
        val test1 = add(SparseArray(3, 0, 1.0f)(1 -> 3), SparseArray(5, 0, 1.0f)(2 -> 10))
        assert(test1(0) === 0)
        assert(test1(1) === 3)
        assert(test1(2) === 10)
        assert(test1(3) === 0)
        assert(test1(4) === 0)
        assert(test1.length() === 5)
        assert(test1.density() === 2/5f)

        val test2 = add(SparseArray(5, 0, 1.0f)(2 -> 10), SparseArray(3, 0, 1.0f)(1 -> 3))
        assert(test2(0) === 0)
        assert(test2(1) === 3)
        assert(test2(2) === 10)
        assert(test2(3) === 0)
        assert(test2(4) === 0)
        assert(test2.length() === 5)
        assert(test2.density() === 2/5f)
      }

      it("should have the threshold of the parent, if the contents stay below said threshold") {
        val test = add(SparseArray(3, 0, 0.5f)(0 -> 2), SparseArray(3, 0, 0.5f)(0 -> 4))
        assert(test.materializationThreshold === 0.5f)
        assert(test.density() === 1/3f)
        assert(!test.isMaterialized)
      }

      it("should have been materialized if the parent threshold is crossed") {
        val test = add(SparseArray(3, 0, 0.5f)(0 -> 2), SparseArray(3, 0, 0.5f)(1 -> 4))
        assert(test.materializationThreshold === 0.5f)
        assert(test.density() === 1.0f)
        assert(test.isMaterialized)
      }

      it("should not be materialized if parent thresholds are crossed, but a higher threshold is given") {
        val test = SparseArray.merge((a: Int, b: Int) => a + b, Some(1.0f))(
          SparseArray(3, 0, 0.5f)(0 -> 2),
          SparseArray(3, 0, 0.5f)(1 -> 4)
        )
        assert(test.materializationThreshold === 1.0f)
        assert(test.density === 2/3f)
        assert(!test.isMaterialized)
      }

      it("should be materialized if parent thresholds are not crossed, but a lower threshold is given") {
        val test = SparseArray.merge((a: Int, b: Int) => a + b, Some(0.25f))(
          SparseArray(3, 0, 0.5f)(0 -> 2),
          SparseArray(3, 0, 0.5f)(0 -> 4)
        )
        assert(test.materializationThreshold === 0.25f)
        assert(test.density === 1.0f)
        assert(test.isMaterialized)
      }
    }

    describe("#map") {
      it("Should work fine on a dense array") {
        val a = SparseArray(3, 0, 0.0f)(0 -> 1, 2 -> 4)
        val b = a.map(n => n*n)
        assert(b.isMaterialized)
        assert(b.density === 1.0f)
        assert(b.materializationThreshold === 0.0f)
        assert(b(0) === 1)
        assert(b(1) === 0)
        assert(b(2) === 16)
        assert(b.default() === 0)
      }

      it("Should work fine on a sparse array") {
        val a = SparseArray(3, 0, 1.0f)(0 -> 1, 2 -> 4)
        val b = a.map(n => n*n)
        assert(!b.isMaterialized)
        assert(b.density === 2/3f)
        assert(b.materializationThreshold === 1.0f)
        assert(b(0) === 1)
        assert(b(1) === 0)
        assert(b(2) === 16)
        assert(b.default() === 0)
      }
    }

    describe("#flatMap") {
      it("Should work fine on a dense array") {
        val a = SparseArray(5, List[String](), 0.0f)(3 -> List("abc", "def"), 1 -> List("ghi", "jkl"))
        assert(List("ghi", "jkl", "abc", "def") === a.flatMap(list => list).toList)
      }
      it("Should work fine on a sparse array") {
        val a = SparseArray(5, List[String](), 1.0f)(3 -> List("abc", "def"), 1 -> List("ghi", "jkl"))
        assert(List("ghi", "jkl", "abc", "def") === a.flatMap(list => list).toList)
      }
      it("Should include default values when the input function doesn't map them to empty iterables") {
        val a = SparseArray(5, List[String](), 1.0f)(3 -> List("abc", "def"), 1 -> List("ghi", "jkl"))
        assert(List("mno", "ghi", "jkl", "mno", "mno", "abc", "def", "mno", "mno") === a.flatMap(list => list :+ "mno").toList)
      }
    }
    describe("#mapWithIndex") {
      it("Should work fine on a dense array") {
        val a = SparseArray(3, 0, 0.0f)(0 -> 1, 2 -> 4)
        val b = a.mapWithIndex((n, i) => n*n + i)
        assert(b.isMaterialized)
        assert(b.density() === 1.0f)
        assert(b.materializationThreshold === 0.0f)
        assert(b(0) === 1)
        assert(b(1) === 1)
        assert(b(2) === 18)
        assert(b.default() === -1)
      }

      it("Should work fine on a sparse array") {
        val a = SparseArray(3, 0, 1.0f)(0 -> 1, 2 -> 4)
        val b = a.mapWithIndex((n, i) => n*n + i)
        assert(!b.isMaterialized)
        assert(b.density === 2/3f)
        assert(b.materializationThreshold === 1.0f)
        assert(b(0) === 1)
        assert(b(1) === -1)
        assert(b(2) === 18)
        assert(b.default() === -1)
      }
    }

    describe("#default") {
      it("should not change when its type is mutable and it is used as a basis for non-default values") {
        val sa = SparseArray(3, Buffer[Int]())()
        sa(0) = sa(0) += 2
        sa(1) = sa(1) += 3
        assert(List(2) === sa(0).toList)
        assert(List(3) === sa(1).toList)
        assert(List[Int]() === sa(2).toList)
      }
      it("should not change when its type is mutable, etc, even after being mapped.") {
        val sa = SparseArray(4, Buffer[Int]())()
        sa(0) = sa(0) += 2
        val sa2 = sa.map(_.map(n => n*n))
        sa2(1) = sa2(1) += 3
        sa2(2) = sa2(2) += 5
        assert(List(2) === sa(0).toList)
        assert(List[Int]() === sa(1).toList)
        assert(List[Int]() === sa(2).toList)
        assert(List[Int]() === sa(3).toList)
        assert(List(4) === sa2(0).toList)
        assert(List(3) === sa2(1).toList)
        assert(List(5) === sa2(2).toList)
        assert(List[Int]() === sa2(3).toList)
      }
    }

    describe("#seq") {
      it("Should create a materialized sequence given a sparse array, without materializing the sparse array") {
        val sa = SparseArray(3, 0, 1.0f)(0 -> 1, 2 -> 4)
        val seq = sa.seq
        assert(!sa.isMaterialized)
        assert(seq.toList === List(1, 0, 4))
      }

      it("Should work on a materialized sparse array") {
        val sa = SparseArray(3, 0, 0.0f)(0 -> 1, 2 -> 4)
        assert(sa.isMaterialized)
        val seq = sa.seq
        assert(seq.toList === List(1, 0, 4))
      }
    }

    describe("#head") {
      it("should return the default when element 0 isn't set") {
        val sa = SparseArray(3, 0, 0.0f)(1 -> 1, 2 -> 4)
        assert(0 === sa.head)
      }

      it("should return the explicitly set value of element 0 when appropriate") {
        val sa = SparseArray(3, 0, 0.0f)(0 -> 1, 2 -> 4)
        assert(1 === sa.head)
      }
    }
  }
}
