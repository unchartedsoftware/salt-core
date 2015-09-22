package com.unchartedsoftware.mosaic.core.projection.numeric

import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import org.apache.spark.sql.Row

/**
 * @param bins number of bins
 * @param minZoom the minimum zoom level which will be passed into rowToCoords()
 * @param maxZoom the maximum zoom level which will be passed into rowToCoords()
 * @param source the ValueExtractor which will extract numeric data-space coordinate values from a Row
 * @param min the minimum value of a data-space coordinate
 * @param max the maximum value of a data-space coordinate
 * @tparam TC the abstract type representing a tile coordinate. Must feature a zero-arg constructor.
 * @tparam DC the abstract type reprseenting a data-space coordinate
 */
abstract class NumericProjection[TC, DC](
  bins: Int,
  minZoom: Int,
  maxZoom: Int,
  val source: ValueExtractor[DC],
  val min: DC,
  val max: DC
) extends Projection[TC](bins, minZoom, maxZoom) {}
