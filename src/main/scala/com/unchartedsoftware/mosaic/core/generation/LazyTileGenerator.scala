package com.unchartedsoftware.mosaic.core.generation

import com.unchartedsoftware.mosaic.core.analytic.{Aggregator, ValueExtractor}
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.generation.request.TileRequest
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.Row
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Produces an RDD[TileData] which only materializes when an operation pulls
 * some or all of those tiles back to the Spark driver
 *
 * @param projection the  projection from data to some space (i.e. 2D or 1D)
 * @param extractor a mechanism for grabbing or synthesizing the "value" column from a source record
 * @param binAggregator the desired bin analytic strategy
 * @param tileAggregator the desired tile analytic strategy
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
abstract class LazyTileGenerator[TC, T, U: ClassTag, V, W, X](
  sc: SparkContext,
  projection: Projection[TC],
  extractor: ValueExtractor[T],
  binAggregator: Aggregator[T, U, V],
  tileAggregator: Aggregator[V, W, X]) {

  /**
   * @param data the RDD containing source data
   * @param request tiles requested for generation
   */
  def generate(data: RDD[Row], request: TileRequest[TC]): RDD[TileData[TC, V, X]]
}
