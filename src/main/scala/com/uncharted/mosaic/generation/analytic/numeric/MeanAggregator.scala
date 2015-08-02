package com.uncharted.mosiac.generation.analytic.numeric

import com.uncharted.mosiac.generation.analytic.Aggregator

class MeanAggregator extends Aggregator[Double, java.lang.Double] {
  val _count = new CountAggregator
  val _sum = new SumAggregator

  override def add(num: Option[Double]): Aggregator[Double, java.lang.Double] = {
    _count.add(num)
    _sum.add(num)
    this
  }

  override def merge(other: Aggregator[Double, java.lang.Double]): Aggregator[Double, java.lang.Double] = {
    other match {
      case o: MeanAggregator => {
        _count.merge(o._count)
        _sum.merge(o._sum)
        this
      }
      case _ => {
        throw new IllegalArgumentException("Cannot merge MeanAggregator with a non-MeanAggregator")
      }
    }
    this
  }

  def value(): java.lang.Double = {
    _sum.value/_count.value
  }

  def reset(): Unit = {
    _sum.reset
    _count.reset
  }
}
