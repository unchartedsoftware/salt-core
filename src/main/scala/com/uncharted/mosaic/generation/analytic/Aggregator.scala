package com.uncharted.mosiac.generation.analytic

/**
 * Subclasses will compute aggregations on values which fall
 * within a bin within a tile, or within a tile.
 *
 * Note that classes implementing this trait ARE NOT responsible
 * for checking if an add()ed number belongs in this bin or tile,
 * or if a merge()d BinAnalytic represents the same bin in the
 * same tile, or the same tile, as this one. Those concerns
 * should be handled at a higher level.
 *
 * T input type
 * U output type
 */
trait Aggregator[T, U] extends Serializable {
  def add(num: Option[T]): Aggregator[T, U]
  def merge(other: Aggregator[T, U]): Aggregator[T, U]
  def value: U
  def reset: Unit
}
