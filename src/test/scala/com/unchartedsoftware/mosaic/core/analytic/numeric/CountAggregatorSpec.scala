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
  }
}
