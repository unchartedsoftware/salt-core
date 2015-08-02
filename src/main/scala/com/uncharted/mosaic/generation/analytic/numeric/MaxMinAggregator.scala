package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

class MaxMinAggregator  extends Aggregator[java.lang.Double, (java.lang.Double, java.lang.Double)] {
  val _max = new MaxAggregator
  val _min = new MinAggregator

  override def add(num: Option[java.lang.Double]): Aggregator[java.lang.Double, (java.lang.Double, java.lang.Double)] = {
    if (num.isDefined) {
      _max.add(Some(num.get.doubleValue))
      _min.add(Some(num.get.doubleValue))
    } else {
      _max.add(None)
      _min.add(None)
    }
    this
  }

  override def merge(other: Aggregator[java.lang.Double, (java.lang.Double, java.lang.Double)]): Aggregator[java.lang.Double, (java.lang.Double, java.lang.Double)] = {
    other match {
      case o: MaxMinAggregator => {
        _max.merge(o._max)
        _min.merge(o._min)
        this
      }
      case _ => {
        throw new IllegalArgumentException("Cannot merge MaxMinAggregator with a non-MaxMinAggregator")
      }
    }

  }

  def value(): (java.lang.Double, java.lang.Double) = {
    (_min.value, _max.value)
  }

  def reset(): Unit = {
    _min.reset
    _max.reset
  }
}
