package com.unchartedsoftware.mosaic.core.analytic.numeric

import org.scalatest._
import com.unchartedsoftware.mosaic.core.analytic.numeric._

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
    }
  }
}
