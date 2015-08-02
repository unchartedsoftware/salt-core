package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

class CountAggregator extends Aggregator[Double, java.lang.Double] {
  var _count = 0D

  override def add(num: Option[Double]): Aggregator[Double, java.lang.Double] = {
    _count += 1
    this
  }
  override def merge(other: Aggregator[Double, java.lang.Double]): Aggregator[Double, java.lang.Double] = {
    other match {
      case o: CountAggregator => {
        _count += o.value
        this
      }
      case _ => {
        throw new IllegalArgumentException("Cannot merge CountAggregator with a non-CountAggregator")
      }
    }
    this
  }

  def value(): java.lang.Double = {
    _count
  }

  def reset(): Unit = {
    _count = 0D
  }
}
