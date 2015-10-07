package com.unchartedsoftware.mosaic.core.generation

import com.unchartedsoftware.mosaic.core.analytic.Aggregator
import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.generation.request.TileRequest
import org.apache.spark.SparkContext
import org.apache.spark.sql.Row
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag

/**
 * Represents a ValueExtractor -> Projection -> binAggregator -> tileAggregator
 *                            ValueExtractor --------^
 * Multiple series are meant to be tiled by a TileGenerator simultaneously
 *
 * @param cExtractor a mechanism for grabbing the data-space coordinates from a source record
 * @param projection the  projection from data to some space (i.e. 2D or 1D)
 * @param vExtractor a mechanism for grabbing or synthesizing the "value" column from a source record (optional)
 * @param binAggregator the desired bin analytic strategy
 * @param tileAggregator the desired tile analytic strategy (optional)
 * @tparam DC the abstract type representing a data-space coordinate
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam BC the abstract type representing a bin coordinate. Must feature a zero-arg
 *            constructor and should be something that can be represented in 1 dimension.
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class Series[DC, TC, BC, T, U, V, W, X](
  val tileSize: BC,
  val cExtractor: ValueExtractor[DC],
  val projection: Projection[DC,TC,BC],
  val vExtractor: Option[ValueExtractor[T]],
  val binAggregator: Aggregator[T, U, V],
  val tileAggregator: Option[Aggregator[V, W, X]]) extends Serializable {
}
