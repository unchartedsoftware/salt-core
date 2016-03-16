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

package software.uncharted.salt.core.generation

import org.scalatest._
import software.uncharted.salt.Spark
import software.uncharted.salt.core.generation.request._
import software.uncharted.salt.core.projection._
import org.apache.spark.sql.Row

class TileGeneratorSpec extends FunSpec {
  describe("TileGenerator") {
    describe("#apply()") {
      it("should return the default TileGenerator implementation") {
        assert(TileGenerator(Spark.sc).isInstanceOf[rdd.RDDTileGenerator])
      }
    }
  }
}
