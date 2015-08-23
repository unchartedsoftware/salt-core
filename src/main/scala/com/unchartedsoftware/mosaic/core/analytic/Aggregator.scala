package com.unchartedsoftware.mosaic.core.analytic

/**
 * Subclasses will provide functions for computing online
 * aggregations on values which fall within a bin, or within a tile.
 *
 * @tparam I input type
 * @tparam N intermediate type
 * @tparam O output type
 */
trait Aggregator[-I, N, O] extends Serializable {
  def default(): N
  def add(current: N, next: Option[I]): N
  def merge(left: N, right: N): N
  def finish(intermediate: N): O
}
