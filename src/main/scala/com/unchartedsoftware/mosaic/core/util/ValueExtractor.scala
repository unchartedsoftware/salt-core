package com.unchartedsoftware.mosaic.core.util

import org.apache.spark.sql.Row

/**
 * An anonymous implementation of this trait provides logic for extracting/synthesizing a value from a Row
 * @tparam T the type of the value which will be extracted/synthesized from Row
 */
trait ValueExtractor[T] extends Serializable {
  /**
   * @return the value column from a given Row, or none if no value column is specified
   */
  def rowToValue(r: Row): Option[T]
}
