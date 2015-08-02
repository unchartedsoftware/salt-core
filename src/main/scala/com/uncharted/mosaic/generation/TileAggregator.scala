package com.uncharted.mosiac.generation

import com.uncharted.mosiac.generation.analytic.{ValueExtractor, Analytic, Aggregator}
import com.uncharted.mosiac.generation.projection.Projection
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.Row

/**
 * Subclasses aggregate values within a tile into xBins x yBins
 * Note that this class IS NOT aware of whether or not a row belongs
 * within the tile it represents. This concern should be handled at
 * a higher level.
 *
 * @param coords the tile coordinates for this aggregator
 * @param bProjection the (broadcasted) projection from data to some space (i.e. 2D or 1D)
 * @param bExtractor a (broadcasted) mechanism for grabbing the "value" column from a source record
 * @param bAnalytic the (broadcasted) desired binning strategy
 * @tparam T Input data type for aggregators
 * @tparam U Output data type for bin aggregators
 * @tparam V Output data type for tile aggregators
 */
class TileAggregator[T, U, V](
  private var coords:(Int, Int, Int),
  val bProjection: Broadcast[Projection],
  val bExtractor: Broadcast[ValueExtractor[T]],
  val bAnalytic: Broadcast[Analytic[T, U, V]]) extends Serializable {

  val projection:Projection = bProjection.value
  val extractor:ValueExtractor[T] = bExtractor.value
  val analytic:Analytic[T,U,V] = bAnalytic.value

  val _tile = analytic.makeTileAggregator
  var _binsTouched = 0

  val _bins = new Array[Aggregator[T, U]](projection.xBins*projection.yBins)
  for (i <- 0 until projection.xBins*projection.yBins) {
    _bins(i) = analytic.makeBinAggregator
  }
  val binTouchMap = new Array[Boolean](projection.xBins*projection.yBins)

  def add(bin: (Int, Int), row: Row): TileAggregator[T, U, V] = {
    val value = extractor.rowToValue(row)
    val index = bin._1 + bin._2*projection.xBins
    val b = _bins(index)
    if (!binTouchMap(index)) {
      binTouchMap(index) = true
      _binsTouched+=1
    }
    b.add(value)
    _tile.add(Some(b.value)) //TODO should this happen once at the end, across all bins?
    this
  }

  def merge(other: TileAggregator[T, U, V]): TileAggregator[T, U, V] = {
    for (i <- 0 until projection.xBins*projection.yBins) {
      _bins(i).merge(other._bins(i))
    }
    _tile.merge(other._tile)
    this
  }

  def getBinAggregator(bin: (Int, Int)): Aggregator[T, U] = {
    _bins(bin._1 + bin._2*projection.xBins)
  }

  def getTileAggregator: Aggregator[U, V] = {
    _tile
  }

  def binsTouched: Int = {
    _binsTouched
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
    _binsTouched = 0
    for (i <- 0 until binTouchMap.length) {
      binTouchMap(i) = false
    }
    for (i <- 0 until _bins.length) {
      _bins(i).reset
    }
    _tile.reset
  }
}
