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

package software.uncharted.salt.core.generation

import software.uncharted.salt.core.analytic.Aggregator
import software.uncharted.salt.core.projection.Projection
import software.uncharted.salt.core.generation.output.{SeriesData,Tile}
import software.uncharted.salt.core.generation.request.TileRequest
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag

/**
 * Produces an RDD[SeriesData] which only materializes when an operation pulls
 * some or all of those tiles back to the Spark driver
 *
 * @param sc a SparkContext
 */
abstract class TileGenerator(sc: SparkContext) {

  /**
   * @param data the RDD containing source data
   * @param series a Seq of matching sets of ValueExtractors+Projection+Aggregators, which
   *               represent different aggregations of identical data within a tile (into
   *               different quantities of bins, using different columns, and/or different
   *               aggregation functions). All Series will be generated simultaneously via
   *               a single pass over the source data.
   * @param request tiles requested for generation
   * @tparam RT the source data record type (the source data is an RDD[RT])
   * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
   */
  def generate[RT,TC: ClassTag](data: RDD[RT], series: Seq[Series[RT,_,TC,_,_,_,_,_,_]], request: TileRequest[TC]): RDD[Tile[TC]]
}
