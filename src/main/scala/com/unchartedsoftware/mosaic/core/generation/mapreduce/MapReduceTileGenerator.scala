package com.unchartedsoftware.mosaic.core.generation.mapreduce

import com.unchartedsoftware.mosaic.core.analytic.Aggregator
import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.generation.output.TileData
import com.unchartedsoftware.mosaic.core.generation.TileGenerator
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
   * rows which contain a key(series id, tile coordinate), a bin and a value.
   */
  private def transformData(
    data: RDD[Row],
    bSeries: Broadcast[Seq[MapReduceSeries[_,TC,_,_,_,_,_,_]]],
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

  def generate(data: RDD[Row], series: Seq[MapReduceSeries[_,TC,_,_,_,_,_,_]], request: TileRequest[TC]): RDD[Seq[TileData[TC,_,_]]] = {
    data.cache //ensure data is cached

    //broadcast stuff we'll use on the workers throughout our tilegen process
    val bSeries = sc.broadcast(series)
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
    bSeries: Broadcast[Seq[MapReduceSeries[_,TC,_,_,_,_,_,_]]]
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
        buff.append(series(s).finish(t._2(s)))
      }
      buff.toSeq
    })
  }
}

private class MapReduceTileGeneratorCombiner[TC](
  bSeries: Broadcast[Seq[MapReduceSeries[_,TC,_,_,_,_,_,_]]]) extends Serializable {

  //create a new combiner, with a fresh set of bins
  def createCombiner(firstValue: (Int, (Int, Option[_]))): Array[Array[_]] = {
    val buff = new ArrayBuffer[Array[_]]
    val series = bSeries.value
    for (s <- 0 until series.length) {
      var b = series(s).makeBins
      if (firstValue._1 == s) {
        b = series(s).add(b, firstValue._2)
      }
      buff.append(b)
    }
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
