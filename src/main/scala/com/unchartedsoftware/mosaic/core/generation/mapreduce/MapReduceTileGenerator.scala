package com.unchartedsoftware.mosaic.core.generation.accumulator

import com.unchartedsoftware.mosaic.core.analytic.{Aggregator, ValueExtractor}
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.generation.BatchTileGenerator
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{Row, DataFrame}
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Tile Generator which utilizes a map/reduce approach to
 * generating large tile sets.
 * @param projection the  projection from data to some space (i.e. 2D or 1D)
 * @param extractor a mechanism for grabbing the "value" column from a source record
 * @param binAggregator the desired bin analytic strategy
 * @param tileAggregator the desired tile analytic strategy
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class MapReduceTileGenerator[TC: ClassTag, T, U: ClassTag, V, W, X](
  sc: SparkContext,
  projection: Projection[TC],
  extractor: ValueExtractor[T],
  binAggregator: Aggregator[T, U, V],
  tileAggregator: Aggregator[V, W, X])(implicit tileCoordManifest: Manifest[TC])
extends BatchTileGenerator[TC, T, U, V, W, X](sc, projection, extractor, binAggregator, tileAggregator) {

  /**
   * Converts raw input data into an RDD which contains only what we need:
   * rows which contain a tile coordinate and a value.
   */
  private def transformData(
    dataFrame: DataFrame,
    bProjection: Broadcast[Projection[TC]],
    bExtractor: Broadcast[ValueExtractor[T]]): RDD[(TC, Option[T])] = {
    throw new UnsupportedOperationException("TODO")
    // dataFrame.mapPartitions(rows => rows.map(
    //   r => {
    //     bProjection.
    //   }
    // ))
  }

  def generate(dataFrame: DataFrame, levels: Seq[Int]): RDD[TileData[TC, V, X]] = {
    throw new UnsupportedOperationException("TODO")
    // dataFrame.cache //ensure data is cached
    //
    // //broadcast stuff we'll use on the workers throughout our tilegen process
    // val bProjection = sc.broadcast(projection)
    // val bExtractor = sc.broadcast(extractor)
    // val bBinAggregator = sc.broadcast(binAggregator)
    // val bTileAggregator = sc.broadcast(tileAggregator)
    //
    // //Start by transforming the raw input data into a distributed set of rows (coord, value)
    // transformData(dataFrame, bProjection, bExtractor)
    //
    // bProjection.unpersist
    // bExtractor.unpersist
    // bBinAggregator.unpersist
    // bTileAggregator.unpersist
  }

}
