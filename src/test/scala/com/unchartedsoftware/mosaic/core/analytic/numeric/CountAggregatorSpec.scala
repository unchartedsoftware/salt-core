package com.unchartedsoftware.mosaic.core.analytic.numeric

import org.scalatest._
import com.unchartedsoftware.mosaic.core.analytic.numeric._

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
      it("should convert the intermediate value into a java Double") {
        var test = Math.random
        assert(CountAggregator.finish(test).isInstanceOf[java.lang.Double])
        assert(CountAggregator.finish(test).equals(new java.lang.Double(test)))
      }
    }
  }
}
