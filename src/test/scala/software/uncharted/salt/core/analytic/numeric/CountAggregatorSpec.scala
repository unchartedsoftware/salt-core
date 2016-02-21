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

class CountAggregatorSpec extends FunSpec {
  describe("CountAggregator") {
    describe("#default()") {
      it("should have a default value of 0") {
        assert(CountAggregator.default === 0D)
      }
    }

    describe("#add()") {
      it("should add 1 to the existing count when a new value is passed in") {
        assert(CountAggregator.add(CountAggregator.default, Some(1)) === 1D)
        assert(CountAggregator.add(CountAggregator.default, Some("hello")) === 1D)
      }
      it("should add 1 to the existing count when a null record is passed in") {
        assert(CountAggregator.add(CountAggregator.default, None) === 1D)
      }
    }

    describe("#merge()") {
      it("should combine two counts using addition") {
        var left = Math.random
        var right = Math.random
        assert(CountAggregator.merge(left, right) === left + right)
      }
    }

    describe("#finish()") {
      it("should convert the intermediate value into a Double") {
        var test = Math.random
        assert(CountAggregator.finish(test).isInstanceOf[Double])
        assert(CountAggregator.finish(test).equals(test))
      }
    }
  }
}
