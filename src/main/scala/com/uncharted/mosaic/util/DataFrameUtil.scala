package com.uncharted.mosaic.util

import java.sql.{Date, Timestamp}
import org.apache.spark.sql.Row

/**
 * Common operations for DataFrames which compose our source data, such as
 * the retrieval of sample rows or the computation of summary statistics.
 */
object DataFrameUtil extends Serializable {
  def getDouble(col: Int, r: Row): Double = {
    r.get(col) match {
      case d: Double => d
      case t: Timestamp => (t.getTime).toDouble
      case l: Long => l.toDouble
      case f: Float => f.toDouble
      case i: Int => i.toDouble
      case s: Short => s.toDouble
      case d: Date => (d.getTime).toDouble
      case b: Byte => b.toDouble
      case _ => {
        val colType = r.get(col).getClass.getName
        throw new IllegalArgumentException(s"Column $col is non-numeric (type: $colType)")
      }
    }
  }
}
