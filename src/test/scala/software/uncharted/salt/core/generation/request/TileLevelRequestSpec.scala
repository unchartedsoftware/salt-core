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
import software.uncharted.salt.core.generation.request._
import software.uncharted.salt.core.projection._
import org.apache.spark.sql.Row

class TileLevelRequestSpec extends FunSpec {
  describe("TileLevelRequest") {
    describe("#inRequest()") {
      val request = new TileLevelRequest(Seq(0,1,2,3,4,5), (t: (Int, Int)) => t._1)
      for (i <- 0 until 100) {
        val level = (Math.random*10).toInt
        if (level < 6) {
          assert(request.inRequest((level, 0)))
        } else {
          assert(!request.inRequest((level, 0)))
        }
      }
    }
  }
}
