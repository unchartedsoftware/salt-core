package com.uncharted.mosiac.generation.analytic

/**
 * A pair of factories which produce a bin Aggregator and a tile Aggregator
 * @tparam T the input value type for both Aggregators
 * @tparam U the output type for the bin Aggregator
 * @tparam V the output type for the tile Aggregator
 */
trait Analytic[T, U, V] extends Serializable {
  def makeBinAggregator(): Aggregator[T, U]
  def makeTileAggregator(): Aggregator[U, V]
}
