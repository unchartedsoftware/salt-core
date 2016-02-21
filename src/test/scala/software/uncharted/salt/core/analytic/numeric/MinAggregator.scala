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

class MinAggregatorSpec extends FunSpec {
  describe("MinAggregator") {
    describe("#default()") {
      it("should have a default value of NaN") {
        assert(MinAggregator.default.equals(Double.NaN))
      }
    }

    describe("#add()") {
      it("should return Double.MaxValue if a NaN is added to the default value") {
        assert(MinAggregator.add(MinAggregator.default, Some(Double.NaN)) === Double.MaxValue)
      }
      it("should retain the current value when a null record is passed in") {
        assert(MinAggregator.add(MinAggregator.default, None).equals(MinAggregator.default))
        var test = Math.random
        assert(MinAggregator.add(test, None).equals(test))
      }
      it("should min() the current value and new value when a new value is passed in") {
        var test = Math.random
        assert(MinAggregator.add(MinAggregator.default, Some(test)) === test)

        var test2 = Math.random
        assert(MinAggregator.add(test, Some(test2)) === Math.min(test, test2))
      }
    }

    describe("#merge()") {
      it("should combine two counts using min()") {
        var left = Math.random()
        var right = Math.random()
        assert(MinAggregator.merge(left, right) === Math.min(left, right))
      }

      it("should ignore NaNs in favour of actual values") {
        var left = Double.NaN
        var right = Math.random
        assert(MinAggregator.merge(left, right) === right)
        assert(MinAggregator.merge(right, left) === right)
        assert(MinAggregator.merge(left, left).equals(left))
      }
    }

    describe("#finish()") {
      it("should convert the intermediate value into a Double") {
        var test = Math.random
        assert(MinAggregator.finish(test).isInstanceOf[Double])
        assert(MinAggregator.finish(test).equals(test))
      }
    }
  }
}
