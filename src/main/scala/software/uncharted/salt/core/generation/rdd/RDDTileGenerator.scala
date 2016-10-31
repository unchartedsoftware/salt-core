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

package software.uncharted.salt.core.generation.rdd

import software.uncharted.salt.core.analytic.Aggregator
import software.uncharted.salt.core.projection.Projection
import software.uncharted.salt.core.generation.output.{SeriesData,Tile}
import software.uncharted.salt.core.generation.{Series, TileGenerator}
import software.uncharted.salt.core.generation.request.TileRequest
import software.uncharted.salt.core.util.SparseArray
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import scala.reflect.ClassTag
import scala.collection.SeqView
import scala.collection.mutable.{ListBuffer, ArrayBuffer, HashMap}
import scala.util.Try

/**
 * Tile Generator which uses an RDD combineByKey approach to efficiently
 * generate large tile sets.
 *
 * @param sc a SparkContext
 * @tparam TC the abstract type representing a tile coordinate.
 */
class RDDTileGenerator(sc: SparkContext) extends TileGenerator(sc) {

  /**
   * Converts raw input data into an RDD which contains only what we need:
   * rows which contain a key (tile coordinate), a series index, and a (bin, value).
   */
  private def transformData[RT,TC: ClassTag](
    data: RDD[RT],
    bSeries: Broadcast[Seq[RDDSeriesWrapper[RT,_,TC,_,_,_,_,_,_]]],
    bRequest: Broadcast[TileRequest[TC]]): RDD[(TC, (Int, Int, Option[_]))] = {

    // this is a critical path, so we're not going to use idiomatic scala here.
    data.flatMap(r => {
      val seriesSeq = bSeries.value;
      val buff = new ListBuffer[(TC,(Int, Int, Option[_]))]()
      var index: Int = 0;
      while (index < seriesSeq.length) {
        val d = seriesSeq(index)
                .projectAndFilter(r, bRequest)
                .map((c: (TC, Int, Option[_])) => (c._1, (index, c._2, c._3)))
        buff.appendAll(d)
        index+=1;
      }
      buff.toList
    })
  }

  override def generate[RT,TC: ClassTag](data: RDD[RT], series: Seq[Series[RT,_,TC,_,_,_,_,_,_]], request: TileRequest[TC]): RDD[Tile[TC]] = {
    val mSeries = series.map(s => new RDDSeriesWrapper(s))

    //broadcast stuff we'll use on the workers throughout our tilegen process
    val bSeries = sc.broadcast(mSeries)
    val bRequest = sc.broadcast(request)

    //Start by transforming the raw input data into a distributed RDD
    val transformedData = transformData[RT,TC](data, bSeries, bRequest)

    //Now we are going to use combineByKey, but we need some lambdas which are
    //defined in RDDTileGeneratorCombiner first.
    val combiner = new RDDTileGeneratorCombiner[RT,TC](bSeries)

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
    transformedData: RDD[(TC, (Int, Int, Option[_]))],
    combiner: RDDTileGeneratorCombiner[RT,TC],
    bSeries: Broadcast[Seq[RDDSeriesWrapper[RT,_,TC,_,_,_,_,_,_]]]
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
      val buff = new HashMap[String, SeriesData[TC,_,_,_]]
      for(s <- 0 until series.length) {
        buff += (series(s).id -> series(s).finish((t._1, t._2(s))))
      }
      new Tile(t._1, buff)
    })
  }
}

//Just an easy way to define closures for combineByKey
private class RDDTileGeneratorCombiner[RT,TC](
  bSeries: Broadcast[Seq[RDDSeriesWrapper[RT,_,TC,_,_,_,_,_,_]]]) extends Serializable {

  //create a new combiner, with a fresh set of bins
  def createCombiner(firstValue: (Int, Int, Option[_])): Array[SparseArray[_]] = {
    val buff = new ArrayBuffer[SparseArray[_]]
    val series = bSeries.value
    //create empty bins with default values, for each series
    var i: Int = 0;
    while (i < series.length) {
      var b = series(i).makeBins
      buff.append(b)
      i+=1;
    }
    //add the first value to the appropriate set of bins
    series(firstValue._1).add(buff(firstValue._1), firstValue._2, firstValue._3)
    buff.toArray
  }

  //how to add a value to a combiner
  def mergeValue(combiner: Array[SparseArray[_]], newValue: (Int, Int, Option[_])): Array[SparseArray[_]] = {
    bSeries.value(newValue._1).add(combiner(newValue._1), newValue._2, newValue._3)
    combiner
  }

  //how to merge combiners
  def mergeCombiners(r1: Array[SparseArray[_]], r2: Array[SparseArray[_]]): Array[SparseArray[_]] = {
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
 * Wrapper methods allow the RDDTileGenerator to ignore the types within
 * individual series.
 * @tparam RT the source data record type (the source data is an RDD[RT])
 * @tparam DC the abstract type representing a data-space coordinate
 * @tparam TC the abstract type representing a tile coordinate.
 * @tparam BC the abstract type representing a bin coordinate. Must be represented in 1 dimension.
 * @tparam T Input data type for bin aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
private class RDDSeriesWrapper[
  RT,
  DC,
  TC,
  BC,
  @specialized(Int, Long, Double) T,
  @specialized(Int, Long, Double) U,
  @specialized(Int, Long, Double) V,
  W,
  X
]
(series: Series[RT, DC, TC, BC, T, U, V, W, X])
(
  implicit val binIntermediateTag: ClassTag[U],
  implicit val binFinalTag: ClassTag[V],
  implicit val tileIntermediateTag: ClassTag[W]
) extends Serializable {

  private[salt] def id: String = {
    series.id
  }

  private val maxBins = series.projection.binTo1D(series.maxBin, series.maxBin) + 1

  /**
   * Combines cExtractor with projection and request to produce an intermediate
   * result for each Row which is useful to a TileGenerator
   *
   * @param row a record type to project and retrieve a value from for aggregation
   * @return Traversable[(TC, Int, Option[T])] a Seq of (tile coordinate,1D bin index,extracted value) tuples
   */
  def projectAndFilter(row: RT, bRequest: Broadcast[TileRequest[TC]]): Traversable[(TC, Int, Option[T])] = {
    // get the value for this row
    val value = series.vExtractor(row)

    series.projection.project(series.cExtractor(row), series.maxBin)
    .map(coords => {
      // spread value over coordinates with spreading function if there is one
      val spreadValues = series.spreadingFunction.isDefined match {
        case true => {
          val spreadValues = series.spreadingFunction.get.spread(coords, value)
          spreadValues.view.map(c => (c._1, series.projection.binTo1D(c._2, series.maxBin), c._3))
        }
        case _ => coords.view.map(c => (c._1, series.projection.binTo1D(c._2, series.maxBin), value))
      }
      //filter down to only the tiles we care about
      spreadValues.filter(d =>  bRequest.value.inRequest(d._1)).force
    })
    //return an empty sequence if our value at this point is None
    .getOrElse(Seq[(TC, Int, Option[T])]())
  }

  /**
   * @return A fresh buffer to store intermediate values for the bin analytic
   */
  def makeBins(): SparseArray[U] = {
    new SparseArray[U](maxBins, series.binAggregator.default)
  }

  /**
   * Add a value to a buffer using the binAggregator
   * @param buffer an existing buffer
   * @param index a 1D bin coordinate
   * @param newValue an extracted value to aggregate into that bin
   * @return the existing buffer with newValue aggregated into the correct bin
   */
  def add(buffer: SparseArray[_], index: Int, newValue: Option[_]): SparseArray[U] = {
    val uBuffer = buffer.asInstanceOf[SparseArray[U]]
    uBuffer.update(index, series.binAggregator.add(uBuffer(index), newValue.asInstanceOf[Option[T]]))
    uBuffer
  }

  /**
   * Merge two buffers using the binAggregator
   * @param r1 the first buffer
   * @param r2 the second buffer
   * @return a merged buffer. Neither r1 nor r2 should be used after this, since one is reused.
   */
  def merge(r1: SparseArray[_], r2: SparseArray[_]): SparseArray[U] = {
    val uR1 = r1.asInstanceOf[SparseArray[U]]
    val uR2 = r2.asInstanceOf[SparseArray[U]]
    for (i <- 0 until maxBins) {
      uR1.update(i, series.binAggregator.merge(uR1(i), uR2(i)))
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
  def finish(binData: (TC, SparseArray[_])): SeriesData[TC, BC, V, X] = {
    var tile: W = series.tileAggregator match {
      case None => tileIntermediateTag.runtimeClass.newInstance.asInstanceOf[W]
      case _ => series.tileAggregator.get.default
    }
    val key = binData._1

    val typedBinData = binData._2.asInstanceOf[SparseArray[U]]
    val finishedBins = typedBinData.map(series.binAggregator.finish(_))

    series.tileAggregator.foreach { aggregator =>
      for (i <- finishedBins.indices) {
        tile = aggregator.add(tile, Some(finishedBins(i)))
      }
    }

    val finishedTile: Option[X] = series.tileAggregator match {
      case None => None
      case _ => Some(series.tileAggregator.get.finish(tile))
    }
    new SeriesData[TC, BC, V, X](
      series.projection,
      series.maxBin,
      key,
      finishedBins,
      finishedTile
    )
  }
}
