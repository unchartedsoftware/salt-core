package com.unchartedsoftware.mosaic.core.generation.accumulator

import com.unchartedsoftware.mosaic.core.analytic.{Aggregator, ValueExtractor}
import com.unchartedsoftware.mosaic.core.projection.Projection
import scala.reflect.ClassTag
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.{AccumulableParam, Accumulable}
import org.apache.spark.sql.Row
import scala.util.Try
import scala.collection.mutable.HashMap

/**
 * Accumulator which aggregates bin values for a batch of tiles
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @param bExtractor a (broadcasted) mechanism for grabbing the "value" column from a source record
 * @param bBinAggregator the (broadcasted) desired bin analytic strategy
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam T Input data type for aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 */
class TileGenerationAccumulableParam[TC: ClassTag, T, U: ClassTag, V](
    private var bProjection: Broadcast[Projection[TC]],
    private var bExtractor: Broadcast[ValueExtractor[T]],
    private var bBinAggregator: Broadcast[Aggregator[T, U, V]]
  ) extends AccumulableParam[HashMap[TC, Array[U]], (TC, Int, Row)]() {

  def reset(
    newBProjection: Broadcast[Projection[TC]],
    newBExtractor: Broadcast[ValueExtractor[T]],
    newBBinAggregator: Broadcast[Aggregator[T, U, V]]
  ): Unit = {
    bProjection = newBProjection
    bExtractor = newBExtractor
    bBinAggregator = newBBinAggregator
  }

  //will store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }

  override def addAccumulator(r: HashMap[TC, Array[U]], t: (TC, Int, Row)): HashMap[TC, Array[U]] = {
    val tile = t._1
    val bin = t._2
    val row = t._3
    Try({
      val bins = r.get(tile)
      val current = bins.get(bin)
      val value: Option[T] = bExtractor.value.rowToValue(row)
      bins.get(bin) = bBinAggregator.value.add(current, value)
    })
    r
  }

  override def addInPlace(r1: HashMap[TC, Array[U]], r2: HashMap[TC, Array[U]]): HashMap[TC, Array[U]] = {
    val numBins = bProjection.value.bins
    val binAggregator = bBinAggregator.value

    r2.foreach(t => {
      if (r1.contains(t._1)) {
        val r1Bins = r1.get(t._1).get
        for (i <- 0 until numBins) {
          r1Bins(i) = binAggregator.merge(r1Bins(i), t._2(i))
        }
      } else {
        r1.put(t._1, t._2)
      }
    })
    r1
  }

  override def zero(initialValue: HashMap[TC, Array[U]]): HashMap[TC, Array[U]] = {
    val tiles = new HashMap[TC, Array[U]]()
    initialValue.foreach(t => {
      tiles.put(t._1, makeBins(bProjection.value.bins, bBinAggregator.value.default))
    })
    tiles
  }
}
