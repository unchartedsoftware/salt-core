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
import software.uncharted.salt.core.analytic.collection._
import scala.collection.mutable.HashMap

class TopElementsAggregatorSpec extends FunSpec {
  describe("TopElementsAggregator") {
    describe("#default()") {
      it("should have an empty hashmap as a default value") {
        val aggregator = new TopElementsAggregator[String](10)
        assert(aggregator.default.size === 0)
      }
    }

    describe("#add()") {
      it("should count terms when a sequence of new terms is passed in") {
        val aggregator = new TopElementsAggregator[String](10)
        var default = aggregator.default
        aggregator.add(default, Some(Seq("foo", "bar")))
        assert(!default.contains("cab"))
        assert(default.contains("foo"))
        assert(default.contains("bar"))
        assert(default.get("foo").get === 1)
        assert(default.get("bar").get === 1)
        aggregator.add(default, Some(Seq("foo", "bac")))
        assert(default.get("foo").get === 2)
        assert(default.get("bac").get === 1)
      }
      it("should not alter the intermediate value when a null/empty record is passed in") {
        val aggregator = new TopElementsAggregator[String](10)
        var default = aggregator.default
        aggregator.add(default, Some(Seq("foo", "bar")))
        aggregator.add(default, Some(Seq()))
        aggregator.add(default, None)
        assert(!default.contains("cab"))
        assert(default.contains("foo"))
        assert(default.contains("bar"))
        assert(default.get("foo").get === 1)
        assert(default.get("bar").get === 1)
      }
    }

    describe("#merge()") {
      it("should combine two top elements maps using addition") {
        var left = new HashMap[String, Int]
        var right = new HashMap[String, Int]
        val leftFoo = (Math.random*100).toInt
        left.put("foo", leftFoo)
        val leftBar = (Math.random*100).toInt
        left.put("bar", leftBar)
        val rightFoo = (Math.random*100).toInt
        right.put("foo", rightFoo)
        val rightBac = (Math.random*100).toInt
        right.put("bac", rightBac)

        val aggregator = new TopElementsAggregator[String](10)
        val merged = aggregator.merge(left, right)
        assert(!merged.contains("cab"))
        assert(merged.contains("foo"), merged.toString)
        assert(merged.contains("bar"))
        assert(merged.contains("bac"))
        assert(merged.get("foo").get === leftFoo + rightFoo)
        assert(merged.get("bar").get === leftBar)
        assert(merged.get("bac").get === rightBac)
      }
    }

    describe("#finish()") {
      it("should convert the intermediate map into a top N list") {
        val aggregator = new TopElementsAggregator[String](5)
        val intermediate = new HashMap[String, Int]
        intermediate.put("a", 10)
        intermediate.put("b", 5)
        intermediate.put("c", 7)
        intermediate.put("d", 9)
        intermediate.put("e", 12)
        intermediate.put("f", 1)
        val finished = aggregator.finish(intermediate)
        assert(finished.length === 5)
        assert(finished(0) === ("e", 12))
        assert(finished(1) === ("a", 10))
        assert(finished(2) === ("d", 9))
        assert(finished(3) === ("c", 7))
        assert(finished(4) === ("b", 5))
      }
    }
  }
}
