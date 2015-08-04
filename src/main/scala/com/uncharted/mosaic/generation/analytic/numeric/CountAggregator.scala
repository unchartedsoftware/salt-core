package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

object CountAggregator extends Aggregator[Any, Long, java.lang.Double] {

  def default(): Long = {
    0L
  }

  override def add(current: Long, next: Option[Any]): Long = {
    current + 1
  }
  override def merge(left: Long, right: Long): Long = {
    left+right
  }

  def finish(intermediate: Long): java.lang.Double = {
    intermediate.toDouble
  }
}
