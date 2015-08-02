package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

class MaxAggregator extends Aggregator[Double, java.lang.Double] {
  var _max = Double.MinValue

  override def add(num: Option[Double]): Aggregator[Double, java.lang.Double] = {
    if (num.isDefined) {
      _max = Math.max(_max, num.get)
    }
    this
  }
  override def merge(other: Aggregator[Double, java.lang.Double]): Aggregator[Double, java.lang.Double] = {
    other match {
      case o: MaxAggregator => {
        _max = Math.max(_max, o.value)
        this
      }
      case _ => {
        throw new IllegalArgumentException("Cannot merge MaxAggregator with a non-MaxAggregator")
      }
    }
    this
  }

  def value(): java.lang.Double = {
    _max
  }

  def reset(): Unit = {
    _max = Double.MinValue
  }
}
