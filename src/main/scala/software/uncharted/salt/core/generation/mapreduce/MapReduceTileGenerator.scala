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

package software.uncharted.salt.core.generation.mapreduce

import software.uncharted.salt.core.analytic.Aggregator
import software.uncharted.salt.core.projection.Projection
import software.uncharted.salt.core.generation.output.{SeriesData,Tile}
import software.uncharted.salt.core.generation.{Series, TileGenerator}
import software.uncharted.salt.core.generation.request.TileRequest
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag
import scala.collection.mutable.{ArrayBuffer,HashMap}
import scala.util.Try

/**
 * Tile Generator which utilizes a map/reduce approach to
 * generating large tile sets.
 * @param sc a SparkContext
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 */
class MapReduceTileGenerator(sc: SparkContext) extends TileGenerator(sc) {

  /**
   * Converts raw input data into an RDD which contains only what we need:
   * rows which contain a key (tile coordinate), a series ID, and a (bin, value).
   */
  private def transformData[RT,TC: ClassTag](
    data: RDD[RT],
    bSeries: Broadcast[Seq[MapReduceSeriesWrapper[RT,_,TC,_,_,_,_,_,_]]],
    bRequest: Broadcast[TileRequest[TC]]): RDD[(TC, (Int, (Int, Option[_])))] = {

    data.flatMap(r => {
      val request = bRequest.value
      val series = bSeries.value

      //map all input data to an RDD of (TC, (seriesId, 1Dbin, Option[value]))
      val buff = new ArrayBuffer[(TC, (Int, (Int, Option[_])))](series.length)
      for (s <- 0 until series.length) {
        val transformedData = series(s).projectAndTransform(r)
        if (transformedData.isDefined) {
          buff.appendAll(
            transformedData.get
            //filter down to only the tiles we care about
            .filter(d => request.inRequest(d._1))
            //map to include the series ID
            .map(d => (d._1, (s, d._2)))
          )
        }
      }
      buff.toSeq
    })
  }

  override def generate[RT,TC: ClassTag](data: RDD[RT], series: Seq[Series[RT,_,TC,_,_,_,_,_,_]], request: TileRequest[TC]): RDD[Tile[TC]] = {
    data.cache //ensure data is cached

    val mSeries = series.map(s => new MapReduceSeriesWrapper(s))

    //broadcast stuff we'll use on the workers throughout our tilegen process
    val bSeries = sc.broadcast(mSeries)
    val bRequest = sc.broadcast(request)

    //Start by transforming the raw input data into a distributed RDD
    val transformedData = transformData[RT,TC](data, bSeries, bRequest)

    //Now we are going to use combineByKey, but we need some lambdas which are
    //defined in MapReduceTileGeneratorCombiner first.
    val combiner = new MapReduceTileGeneratorCombiner[RT,TC](bSeries)

    //do the work in a closure which is sanitized of all non-serializable things
    val result = sanitizedClosureGenerate[RT,TC](
                    transformedData,
                    combiner,
                    bSeries)

    bRequest.unpersist
    bSeries.unpersist

    result
  }

  private def sanitizedClosureGenerate[RT,TC: ClassTag](
    transformedData: RDD[(TC, (Int, (Int, Option[_])))],
    combiner: MapReduceTileGeneratorCombiner[RT,TC],
    bSeries: Broadcast[Seq[MapReduceSeriesWrapper[RT,_,TC,_,_,_,_,_,_]]]
  ): RDD[Tile[TC]] = {
    //Do the actual combineByKey to produce finished bin data
    val tileData = transformedData.combineByKey(
      combiner.createCombiner,
      combiner.mergeValue,
      combiner.mergeCombiners
    )

    //finish tiles by finishing bins, and applying tile aggregators to bin data
    tileData.map(t => {
      val series = bSeries.value
      val buff = new HashMap[String, SeriesData[TC,_,_]]
      for(s <- 0 until series.length) {
        buff += (series(s).id -> series(s).finish((t._1, t._2(s))))
      }
      new Tile(t._1, buff)
    })
  }
}

//Just an easy way to define closures for combineByKey
private class MapReduceTileGeneratorCombiner[RT,TC](
  bSeries: Broadcast[Seq[MapReduceSeriesWrapper[RT,_,TC,_,_,_,_,_,_]]]) extends Serializable {

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
private class MapReduceSeriesWrapper[RT, DC, TC, BC, T, U: ClassTag, V, W: ClassTag, X](
  series: Series[RT, DC, TC, BC, T, U, V, W, X])(implicit tileIntermediateManifest: Manifest[W]) extends Serializable {

  private[salt] def id: String = {
    series.id
  }

  private val maxBins = series.projection.binTo1D(series.maxBin, series.maxBin) + 1

  /**
   * Combines cExtractor with projection to produce an intermediate
   * result for each Row which is useful to a TileGenerator
   *
   * @param row a record type to project and retrieve a value from for aggregation
   * @return Option[Seq[(TC, (Int, Option[T]))]] an Seq of (tile coordinate,(1D bin index,extracted value)) tuples
   */
  def projectAndTransform(row: RT): Option[Seq[(TC, (Int, Option[T]))]] = {
    val coords = series.projection.project(series.cExtractor(row), series.maxBin)
    if (coords.isDefined) {
      val value: Option[T] = series.vExtractor match {
        case None => None
        case _ => series.vExtractor.get(row)
      }
      if (series.spreadingFunction.isDefined) {
        val spreadValues = series.spreadingFunction.get.spread(coords.get, value)
        Some(spreadValues.map(c => (c._1, (series.projection.binTo1D(c._2, series.maxBin), c._3))))
      } else {
        Some(coords.get.map(c => (c._1, (series.projection.binTo1D(c._2, series.maxBin),value))))
      }
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
   * SeriesData object by finish()ing the bins and applying the
   * tileAggregator
   *
   * @param binData a tuple consisting of a tile coordinate and the intermediate bin values
   * @return a finished SeriesData object
   */
  def finish(binData: (TC, Array[_])): SeriesData[TC, V, X] = {
    var tile: W = series.tileAggregator match {
      case None => tileIntermediateManifest.runtimeClass.newInstance.asInstanceOf[W]
      case _ => series.tileAggregator.get.default
    }
    val key = binData._1
    var binsTouched = 0

    val finishedBins = binData._2.asInstanceOf[Array[U]].map(a => {
      if (!a.equals(series.binAggregator.default)) binsTouched+=1
      val bin = series.binAggregator.finish(a)
      if (series.tileAggregator.isDefined) {
        tile = series.tileAggregator.get.add(tile, Some(bin))
      }
      bin
    })

    val finishedTile: Option[X] = series.tileAggregator match {
      case None => None
      case _ => Some(series.tileAggregator.get.finish(tile))
    }
    new SeriesData[TC, V, X](key, finishedBins, binsTouched, series.binAggregator.finish(series.binAggregator.default), finishedTile, series.projection)
  }
}
