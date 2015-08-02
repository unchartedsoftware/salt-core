package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

class SumAggregator extends Aggregator[Double, java.lang.Double] {
  var _sum = 0D

  override def add(num: Option[Double]): Aggregator[Double, java.lang.Double] = {
    if (num.isDefined) {
      _sum += num.get
    }
    this
  }
  override def merge(other: Aggregator[Double, java.lang.Double]): Aggregator[Double, java.lang.Double] = {
    other match {
      case o: SumAggregator => {
        _sum += o.value
        this
      }
      case _ => {
        throw new IllegalArgumentException("Cannot merge SumAggregator with a non-SumAggregator")
      }
    }
    this
  }

  def value(): java.lang.Double = {
    _sum
  }

  def reset(): Unit = {
    _sum = 0D
  }
}
