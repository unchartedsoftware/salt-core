package com.unchartedsoftware.mosaic.core.generation.mapreduce

import com.unchartedsoftware.mosaic.core.analytic.Aggregator
import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.generation.{Series, TileGenerator}
import com.unchartedsoftware.mosaic.core.generation.request.TileRequest
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.Row
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

/**
 * Tile Generator which utilizes a map/reduce approach to
 * generating large tile sets.
 * @param sc a SparkContext
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 */
class MapReduceTileGenerator[TC: ClassTag]
  (sc: SparkContext)(implicit tileCoordManifest: Manifest[TC])
extends TileGenerator[TC](sc) {

  /**
   * Converts raw input data into an RDD which contains only what we need:
   * rows which contain a key (tile coordinate), a series ID, and a (bin, value).
   */
  private def transformData(
    data: RDD[Row],
    bSeries: Broadcast[Seq[MapReduceSeriesWrapper[_,TC,_,_,_,_,_,_]]],
    bRequest: Broadcast[TileRequest[TC]]): RDD[(TC, (Int, (Int, Option[_])))] = {

    data.flatMap(r => {
      val request = bRequest.value
      val series = bSeries.value

      //map all input data to an RDD of (TC, (seriesId, 1Dbin, Option[value]))
      request.levels
      .flatMap(l => {
        val buff = new ArrayBuffer[(TC, (Int, (Int, Option[_])))](series.length)
        for (s <- 0 until series.length) {
          val transformedData = series(s).projectAndTransform(r, l)
          if (transformedData.isDefined) {
            buff.append(
              //mix in series id as a part of the value
              (transformedData.get._1, (s, transformedData.get._2))
            )
          }
        }
        buff.toSeq
      })
    })
  }

  def generate(data: RDD[Row], series: Seq[Series[_,TC,_,_,_,_,_,_]], request: TileRequest[TC]): RDD[Seq[TileData[TC,_,_]]] = {
    data.cache //ensure data is cached

    val mSeries = series.map(s => new MapReduceSeriesWrapper(s))

    //broadcast stuff we'll use on the workers throughout our tilegen process
    val bSeries = sc.broadcast(mSeries)
    val bRequest = sc.broadcast(request)

    //Start by transforming the raw input data into a distributed RDD
    val transformedData = transformData(data, bSeries, bRequest)

    //Now we are going to use combineByKey, but we need some lambdas which are
    //defined in MapReduceTileGeneratorCombiner first.
    val combiner = new MapReduceTileGeneratorCombiner[TC](bSeries)

    //do the work in a closure which is sanitized of all non-serializable things
    val result = _sanitizedClosureGenerate(
                    transformedData,
                    combiner,
                    bSeries)

    bRequest.unpersist
    bSeries.unpersist

    result
  }

  def _sanitizedClosureGenerate(
    transformedData: RDD[(TC, (Int, (Int, Option[_])))],
    combiner: MapReduceTileGeneratorCombiner[TC],
    bSeries: Broadcast[Seq[MapReduceSeriesWrapper[_,TC,_,_,_,_,_,_]]]
  ): RDD[Seq[TileData[TC,_,_]]] = {
    //Do the actual combineByKey to produce finished bin data
    val tileData = transformedData.combineByKey(
      combiner.createCombiner,
      combiner.mergeValue,
      combiner.mergeCombiners
    )

    //finish tiles by finishing bins, and applying tile aggregators to bin data
    tileData.map(t => {
      val series = bSeries.value
      val buff = new ArrayBuffer[TileData[TC,_,_]](series.length)
      for(s <- 0 until series.length) {
        buff.append(series(s).finish((t._1, t._2(s))))
      }
      buff.toSeq
    })
  }
}

//Just an easy way to define closures for combineByKey
private class MapReduceTileGeneratorCombiner[TC](
  bSeries: Broadcast[Seq[MapReduceSeriesWrapper[_,TC,_,_,_,_,_,_]]]) extends Serializable {

  //create a new combiner, with a fresh set of bins
  def createCombiner(firstValue: (Int, (Int, Option[_]))): Array[Array[_]] = {
    val buff = new ArrayBuffer[Array[_]]
    val series = bSeries.value
    //create empty bins with default values, for each series
    for (s <- 0 until series.length) {
      var b = series(s).makeBins
      buff.append(b)
    }
    //add the first value to the appropriate set of bins
    series(firstValue._1).add(buff(firstValue._1), firstValue._2)
    buff.toArray
  }

  //how to add a value to a combiner
  def mergeValue(combiner: Array[Array[_]], newValue: (Int, (Int, Option[_]))): Array[Array[_]] = {
    bSeries.value(newValue._1).add(combiner(newValue._1), newValue._2)
    combiner
  }

  //how to merge combiners
  def mergeCombiners(r1: Array[Array[_]], r2: Array[Array[_]]): Array[Array[_]] = {
    val series = bSeries.value
    for (s <- 0 until series.length) {
      series(s).merge(r1(s), r2(s))
    }
    r1
  }
}

/**
 * Isolates all type-aware stuff which operates on a Series into a single object
 * 
 * Wrapper methods allow the MapReduceTileGenerator to ignore the types within
 * individual series.
 */
private class MapReduceSeriesWrapper[DC, TC, BC, T, U: ClassTag, V, W, X](
  series: Series[DC, TC, BC, T, U, V, W, X]) extends Serializable {

  private val maxBins = series.projection.binTo1D(series.tileSize, series.tileSize)

  /**
   * Combines cExtractor with projection to produce an intermediate
   * result for each Row which is useful to a TileGenerator
   *
   * @param row a Row to project and retrieve a value from for aggregation
   * @param z the zoom level
   * @return Option[(TC, (Int, Option[T]))] a tile coordinate along with the 1D bin index and the extracted value column
   */
  def projectAndTransform(row: Row, z: Int): Option[(TC, (Int, Option[T]))] = {
    val coord = series.projection.project(series.cExtractor.rowToValue(row), z, series.tileSize)
    if (coord.isDefined) {
      Some(
        (coord.get._1,
          (series.projection.binTo1D(coord.get._2, series.tileSize),series.vExtractor.rowToValue(row))
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
    Array.fill[U](maxBins)(series.binAggregator.default)
  }

  /**
   * Add a value to a buffer using the binAggregator
   * @param buffer an existing buffer
   * @param newValue a 1D bin coordinate and an extracted value to aggregate into that bin
   * @return the existing buffer with newValue aggregated into the correct bin
   */
  def add(buffer: Array[_], newValue: (Int, Option[_])): Array[U] = {
    val uBuffer = buffer.asInstanceOf[Array[U]]
    uBuffer(newValue._1) = series.binAggregator.add(uBuffer(newValue._1), newValue._2.asInstanceOf[Option[T]])
    uBuffer
  }

  /**
   * Merge two buffers using the binAggregator
   * @param r1 the first buffer
   * @param r2 the second buffer
   * @return a merged buffer. Neither r1 nor r2 should be used after this, since one is reused.
   */
  def merge(r1: Array[_], r2: Array[_]): Array[U] = {
    val uR1 = r1.asInstanceOf[Array[U]]
    val uR2 = r2.asInstanceOf[Array[U]]
    for (i <- 0 until maxBins) {
      uR1(i) = series.binAggregator.merge(uR1(i), uR2(i))
    }
    uR1
  }

  /**
   * Converts a set of intermediate bin values into a finished
   * TileData object by finish()ing the bins and applying the
   * tileAggregator
   *
   * @param binData a tuple consisting of a tile coordinate and the intermediate bin values
   * @return a finished TileData object
   */
  def finish(binData: (TC, Array[_])): TileData[TC, V, X] = {
    var tile: W = series.tileAggregator.default
    val key = binData._1
    var binsTouched = 0

    val finishedBins = binData._2.asInstanceOf[Array[U]].map(a => {
      if (!a.equals(series.binAggregator.default)) binsTouched+=1
      val bin = series.binAggregator.finish(a)
      tile = series.tileAggregator.add(tile, Some(bin))
      bin
    })
    new TileData[TC, V, X](key, finishedBins, binsTouched, series.binAggregator.finish(series.binAggregator.default), series.tileAggregator.finish(tile), series.projection)
  }
}
