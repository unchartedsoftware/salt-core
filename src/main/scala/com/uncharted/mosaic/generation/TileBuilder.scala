package com.uncharted.mosiac.generation

import com.uncharted.mosiac.generation.analytic.{ValueExtractor, Aggregator}
import com.uncharted.mosiac.generation.projection.Projection
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.Row
import scala.reflect.ClassTag

import java.util.BitSet

/**
 * Subclasses aggregate values within a tile into xBins x yBins
 * Note that this class IS NOT aware of whether or not a row belongs
 * within the tile it represents. This concern should be handled at
 * a higher level.
 *
 * @param coords the tile coordinates for this aggregator
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @param bExtractor a (broadcasted) mechanism for grabbing the "value" column from a source record
 * @param bBinAggregator the (broadcasted) desired bin analytic strategy
 * @param bTileAggregator the (broadcasted) desired tile analytic strategy
 * @tparam T Input data type for aggregators
 * @tparam U Intermediate data type for bin aggregators
 * @tparam V Output data type for bin aggregators
 * @tparam W Intermediate data type for tile aggregators
 * @tparam X Output data type for tile aggregators
 */
class TileBuilder[T, U: ClassTag, V, W, X](
  private var coords:(Int, Int, Int),
  bProjection: Broadcast[Projection],
  bExtractor: Broadcast[ValueExtractor[T]],
  bBinAggregator: Broadcast[Aggregator[T, U, V]],
  bTileAggregator: Broadcast[Aggregator[V, W, X]]) extends Serializable {

  //will store intermediate values for the bin analytic
  private def makeBins [A:ClassTag] (length: Int, default: A): Array[A] = {
    Array.fill[A](length)(default)
  }
  protected val _bins = makeBins(bProjection.value.xBins*bProjection.value.yBins, bBinAggregator.value.default)

  //tracks stats on how many bins contain non-default values, so
  //we know whether to encode sparse or dense
  protected val _binTouchMap = new BitSet(bProjection.value.xBins*bProjection.value.yBins)

  def add(bin: (Int, Int), row: Row): TileBuilder[T, U, V, W, X] = {
    val value: Option[T] = bExtractor.value.rowToValue(row)
    val index = bin._1 + bin._2*bProjection.value.xBins
    val current = _bins(index)
    if (!_binTouchMap.get(index)) {
      _binTouchMap.set(index)
    }
    _bins(index) = bBinAggregator.value.add(current, value)
    this
  }

  def merge(other: TileBuilder[T, U, V, W, X]): TileBuilder[T, U, V, W, X] = {
    for (i <- 0 until bProjection.value.xBins*bProjection.value.yBins) {
      _bins(i) = bBinAggregator.value.merge(_bins(i), other._bins(i))
    }
    _binTouchMap.and(other._binTouchMap)
    this
  }

  def getBinValue(bin: (Int, Int)): V = {
    bBinAggregator.value.finish(_bins(bin._1 + bin._2*bProjection.value.xBins))
  }

  def getTileAggregator: X = {
    val tileAggregator = bTileAggregator.value
    val binAggregator = bBinAggregator.value
    //will store intermediate values for the tile analytic
    var _tile: W = tileAggregator.default
    for (i <- 0 until _bins.length) {
      _tile = tileAggregator.add(_tile, Some(binAggregator.finish(_bins(i))))
    }
    tileAggregator.finish(_tile)
  }

  def binsTouched: Int = {
    _binTouchMap.cardinality
  }

  def z: Int = {
    coords._1
  }

  def x: Int = {
    coords._2
  }

  def y: Int = {
    coords._3
  }

  def reset(newCoords: (Int, Int, Int)): Unit = {
    coords = newCoords
    _binTouchMap.clear
    for (i <- 0 until _bins.length) {
      _bins(i) = bBinAggregator.value.default
    }
  }
}
