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

package software.uncharted.salt.core.analytic.numeric

import org.scalatest._
import software.uncharted.salt.core.analytic.numeric._

class MaxAggregatorSpec extends FunSpec {
  describe("MaxAggregator") {
    describe("#default()") {
      it("should have a default value of NaN") {
        assert(MaxAggregator.default.equals(Double.NaN))
      }
    }

    describe("#add()") {
      it("should return Double.MinValue if a NaN is added to the default value") {
        assert(MaxAggregator.add(MaxAggregator.default, Some(Double.NaN)) === Double.MinValue)
      }
      it("should retain the current value when a null record is passed in") {
        assert(MaxAggregator.add(MaxAggregator.default, None).equals(MaxAggregator.default))
        var test = Math.random
        assert(MaxAggregator.add(test, None).equals(test))
      }
      it("should max() the current value and new value when a new value is passed in") {
        var test = Math.random
        assert(MaxAggregator.add(MaxAggregator.default, Some(test)) === test)

        var test2 = Math.random
        assert(MaxAggregator.add(test, Some(test2)) === Math.max(test, test2))
      }
    }

    describe("#merge()") {
      it("should combine two counts using max()") {
        var left = Math.random
        var right = Math.random
        assert(MaxAggregator.merge(left, right) === Math.max(left, right))
      }

      it("should ignore NaNs in favour of actual values") {
        var left = Double.NaN
        var right = Math.random
        assert(MaxAggregator.merge(left, right) === right)
        assert(MaxAggregator.merge(right, left) === right)
        assert(MaxAggregator.merge(left, left).equals(left))
      }
    }

    describe("#finish()") {
      it("should convert the intermediate value into a Double") {
        var test = Math.random
        assert(MaxAggregator.finish(test).isInstanceOf[Double])
        assert(MaxAggregator.finish(test).equals(test))
      }
    }
  }
}
