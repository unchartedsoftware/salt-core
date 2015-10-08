package com.unchartedsoftware.mosaic.core.generation

import com.unchartedsoftware.mosaic.core.analytic.Aggregator
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.generation.request.TileRequest
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag

/**
 * Produces an RDD[TileData] which only materializes when an operation pulls
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
  def generate[RT,TC: ClassTag](data: RDD[RT], series: Seq[Series[RT,_,TC,_,_,_,_,_,_]], request: TileRequest[TC]): RDD[Seq[TileData[TC, _, _]]]
}
