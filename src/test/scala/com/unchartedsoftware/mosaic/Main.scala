package com.unchartedsoftware.mosaic

import org.scalatest.tools.Runner

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object Spark {
  val conf = new SparkConf().setAppName("mosaic")
  val sc = new SparkContext(conf)
}

object Main {
  def main(args: Array[String]): Unit = {
    val testResult = Runner.run(Array("-o", "-R", "build/classes/test"))
    if (!testResult) {
      System.exit(1)
    }
  }
}
