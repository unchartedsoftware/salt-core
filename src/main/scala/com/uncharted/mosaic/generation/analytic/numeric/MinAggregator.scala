package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

class MinAggregator extends Aggregator[Double, java.lang.Double] {
  var _min = Double.MaxValue

  override def add(num: Option[Double]): Aggregator[Double, java.lang.Double] = {
    if (num.isDefined) {
      _min = Math.min(_min, num.get)
    }
    this
  }
  override def merge(other: Aggregator[Double, java.lang.Double]): Aggregator[Double, java.lang.Double] = {
    other match {
      case o: MinAggregator => {
        _min = Math.min(_min, o.value)
        this
      }
      case _ => {
        throw new IllegalArgumentException("Cannot merge MinAggregator with a non-MinAggregator")
      }
    }
    this
  }

  def value(): java.lang.Double = {
    _min
  }

  def reset(): Unit = {
    _min = Double.MaxValue
  }
}
