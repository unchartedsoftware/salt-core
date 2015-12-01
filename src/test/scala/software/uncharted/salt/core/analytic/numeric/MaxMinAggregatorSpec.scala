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
import software.uncharted.salt.core.analytic.numeric._

class MinMaxAggregatorSpec extends FunSpec {
  describe("MinMaxAggregator") {
    describe("#default()") {
      it("should have a default intermediate value equal to the default values of the Max and Min aggregators") {
        //.equals doesn't seem to work on Double.NaN through a Tuple2...
        assert(MinMaxAggregator.default._1.equals(MinAggregator.default) &&  MinMaxAggregator.default._2.equals(MaxAggregator.default))
      }
    }

    describe("#add()") {
      it("should return a tuple representing the result of calling add() on a MinAggregator and a MaxAggregator") {
        //.equals doesn't seem to work on Double.NaN through a Tuple2...
        var test = Double.NaN
        var expectedResult = (MinAggregator.add(MinAggregator.default, Some(test)), MaxAggregator.add(MaxAggregator.default, Some(test)))
        var weirdScalaNanHandling = MinMaxAggregator.add(MinMaxAggregator.default, Some(test))
        assert(weirdScalaNanHandling._1.equals(expectedResult._1) && weirdScalaNanHandling._2.equals(expectedResult._2))

        expectedResult = (MinAggregator.add(MinAggregator.default, None), MaxAggregator.add(MaxAggregator.default, None))
        weirdScalaNanHandling = MinMaxAggregator.add(MinMaxAggregator.default, None)
        assert(weirdScalaNanHandling._1.equals(expectedResult._1) && weirdScalaNanHandling._2.equals(expectedResult._2))

        test = Math.random()
        expectedResult = (MinAggregator.add(MinAggregator.default, Some(test)), MaxAggregator.add(MaxAggregator.default, Some(test)))
        assert(MinMaxAggregator.add(MinMaxAggregator.default, Some(test)).equals(expectedResult))
      }
    }

    describe("#merge()") {
      it("should return a tuple representing the result of calling merge() on a MinAggregator and a MaxAggregator") {
        var left = (Math.random, Math.random)
        var right = (Math.random, Math.random)
        assert(MinMaxAggregator.merge(left, right).equals(
          (MinAggregator.merge(left._1, right._1), MaxAggregator.merge(left._2, right._2))
        ))
      }
    }

    describe("#finish()") {
      it("should convert the intermediate value into a (Double, Double) which represents the (min, max)") {
        var test = (Math.random, Math.random)
        assert(MinMaxAggregator.finish(test).isInstanceOf[Tuple2[Double, Double]])
        assert(MinMaxAggregator.finish(test).equals((test._1, test._2)))
      }
    }
  }
}
