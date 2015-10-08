package com.unchartedsoftware.salt.core.generation.output

import com.unchartedsoftware.salt.core.projection.Projection

/**
 * A thin wrapper class for the output of a tile generation
 *
 * @param coords the tile coordinates for this aggregator
 * @param bins the output values of bin aggregators
 * @param binsTouched the number of bins which have non-default values
 * @param tileMeta the output value of the tile aggregator
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam V Output data type for bin aggregators, and input for tile aggregator
 * @tparam X Output data type for tile aggregators
 */
class TileData[TC, V, X](
  val coords: TC,
  val bins: Seq[V],
  val binsTouched: Int,
  val defaultBinValue: V,
  val tileMeta: Option[X],
  val projection: Projection[_,TC,_]) extends Serializable {

  def getBin(bin: Int): V = {
    bins(bin)
  }
}
