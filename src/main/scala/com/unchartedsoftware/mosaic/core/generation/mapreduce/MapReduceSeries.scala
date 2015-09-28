package com.unchartedsoftware.mosaic.core.generation.mapreduce

import com.unchartedsoftware.mosaic.core.analytic.Aggregator
import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.generation.Series
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
 * @param vExtractor a mechanism for grabbing or synthesizing the "value" column from a source record
 * @param binAggregator the desired bin analytic strategy
 * @param tileAggregator the desired tile analytic strategy
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam BC the abstract type representing a bin coordinate. Must feature a zero-arg
 *            constructor and should be something that can be represented in 1 dimension.
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class MapReduceSeries[DC, TC, BC, T, U: ClassTag, V, W, X](
  tileSize: BC,
  cExtractor: ValueExtractor[DC],
  projection: Projection[DC,TC,BC],
  vExtractor: ValueExtractor[T],
  binAggregator: Aggregator[T, U, V],
  tileAggregator: Aggregator[V, W, X])
extends Series(tileSize, cExtractor, projection, vExtractor, binAggregator, tileAggregator) {

  private val maxBins = projection.binTo1D(tileSize, tileSize)

  /**
   * Combines cExtractor with projection to produce an intermediate
   * result for each Row which is useful to a TileGenerator
   *
   * @param row a Row to project and retrieve a value from for aggregation
   * @param z the zoom level
   * @return Option[(TC, (Int, Option[T]))] a tile coordinate along with the 1D bin index and the extracted value column
   */
  def projectAndTransform(row: Row, z: Int): Option[(TC, (Int, Option[T]))] = {
    val coord = projection.project(cExtractor.rowToValue(row), z, tileSize)
    if (coord.isDefined) {
      Some(
        (coord.get._1,
          (projection.binTo1D(coord.get._2, tileSize),vExtractor.rowToValue(row))
        )
      )
    } else {
      None
    }
  }

  /**
   * @return A fresh buffer to store intermediate values for the bin analytic
   */
  def makeBins(): Array[U] = {
    Array.fill[U](maxBins)(binAggregator.default)
  }

  /**
   * Add a value to a buffer using the binAggregator
   * @param buffer an existing buffer
   * @param newValue a 1D bin coordinate and an extracted value to aggregate into that bin
   * @return the existing buffer with newValue aggregated into the correct bin
   */
  def add(buffer: Array[U], newValue: (Int, Option[T])): Array[U] = {
    buffer(newValue._1) = binAggregator.add(buffer(newValue._1), newValue._2)
    buffer
  }

  /**
   * Merge two buffers using the binAggregator
   * @param r1 the first buffer
   * @param r2 the second buffer
   * @return a merged buffer. Neither r1 nor r2 should be used after this, since one is reused.
   */
  def merge(r1: Array[U], r2: Array[U]): Array[U] = {
    for (i <- 0 until maxBins) {
      r1(i) = binAggregator.merge(r1(i), r2(i))
    }
    r1
  }

  /**
   * Converts a set of intermediate bin values into a finished
   * TileData object by finish()ing the bins and applying the
   * tileAggregator
   *
   * @param binData a tuple consisting of a tile coordinate and the intermediate bin values
   * @return a finished TileData object
   */
  def finish(binData: (TC, Array[U])): TileData[TC, V, X] = {
    var tile: W = tileAggregator.default
    val key = binData._1
    var binsTouched = 0

    val finishedBins = binData._2.map(a => {
      if (!a.equals(binAggregator.default)) binsTouched+=1
      val bin = binAggregator.finish(a)
      tile = tileAggregator.add(tile, Some(bin))
      bin
    })
    new TileData[TC, V, X](key, finishedBins, binsTouched, binAggregator.finish(binAggregator.default), tileAggregator.finish(tile), projection)
  }
}
