package com.uncharted.mosiac.generation

import com.uncharted.mosiac.generation.projection.Projection

/**
 * A thin wrapper class for the output of a tile generation
 *
 * @param coords the tile coordinates for this aggregator
 * @param bins the output values of bin aggregators
 * @param binsTouched the number of bins which have non-default values
 * @param tileMeta the output value of the tile aggregator
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam X Output data type for tile aggregators
 */
class TileData[V, X](
  val coords:(Int, Int, Int),
  val bins: Seq[V],
  val binsTouched: Int,
  val defaultBinValue: V,
  val tileMeta: X,
  val projection: Projection) {

  def z: Int = {
    coords._1
  }

  def x: Int = {
    coords._2
  }

  def y: Int = {
    coords._3
  }

  def getBin(x: Int, y:Int): V = {
    bins(x + y*projection.xBins)
  }
}
