package com.unchartedsoftware.mosaic.core.projection.numeric

import com.unchartedsoftware.mosaic.core.projection.Projection
import org.apache.spark.sql.Row

/**
 * @param min the minimum value of a data-space coordinate
 * @param max the maximum value of a data-space coordinate
 * @tparam DC the abstract type representing a data-space coordinate
 * @tparam TC the abstract type representing a tile coordinate. Must feature a
 *            zero-arg constructor.
 * @tparam BC the abstract type representing a bin coordinate. Must feature a zero-arg
 *            constructor and should be something that can be represented in 1 dimension.
 */
abstract class NumericProjection[DC, TC, BC](
  val min: DC,
  val max: DC
) extends Projection[DC, TC, BC]() {}
