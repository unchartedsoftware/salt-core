package software.uncharted.salt.core.analytic.numeric

import org.scalatest._
import software.uncharted.salt.core.analytic.numeric._

class MeanAggregatorSpec extends FunSpec {
  describe("MeanAggregator") {
    describe("#default()") {
      it("should have a default intermediate value equal to the default values of the Sum and Count aggregators") {
        assert(MeanAggregator.default.equals((CountAggregator.default, SumAggregator.default)))
      }
    }

    describe("#add()") {
      it("should return a tuple representing the result of calling add() on a CountAggregator and a SumAggregator") {
        var test = Double.NaN
        var expectedResult = (CountAggregator.add(CountAggregator.default, Some(test)), SumAggregator.add(SumAggregator.default, Some(test)))
        assert(MeanAggregator.add(MeanAggregator.default, Some(test)).equals(expectedResult))

        expectedResult = (CountAggregator.add(CountAggregator.default, None), SumAggregator.add(SumAggregator.default, None))
        assert(MeanAggregator.add(MeanAggregator.default, None).equals(expectedResult))

        test = Math.random()
        expectedResult = (CountAggregator.add(CountAggregator.default, Some(test)), SumAggregator.add(SumAggregator.default, Some(test)))
        assert(MeanAggregator.add(MeanAggregator.default, Some(test)).equals(expectedResult))
      }
    }

    describe("#merge()") {
      it("should return a tuple representing the result of calling merge() on a CountAggregator and a SumAggregator") {
        var left = (Math.round(100*Math.random).toDouble, Math.random)
        var right = (Math.round(100*Math.random).toDouble, Math.random)
        assert(MeanAggregator.merge(left, right).equals(
          (CountAggregator.merge(left._1, right._1), SumAggregator.merge(left._2, right._2))
        ))
      }
    }

    describe("#finish()") {
      it("should convert the intermediate value into a java Double which represents the mean") {
        var test = (Math.round(100*Math.random).toDouble, Math.random)
        assert(MeanAggregator.finish(test).isInstanceOf[java.lang.Double])
        assert(MeanAggregator.finish(test).equals(new java.lang.Double(test._2/test._1)))
      }
    }
  }
}
