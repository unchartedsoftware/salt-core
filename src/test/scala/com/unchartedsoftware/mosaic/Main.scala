package com.unchartedsoftware.mosaic

import org.scalatest.tools.Runner

object Main {
  def main(args: Array[String]): Unit = {
    Runner.run(Array("-o", "-R", "build/classes/test"))
  }
}
