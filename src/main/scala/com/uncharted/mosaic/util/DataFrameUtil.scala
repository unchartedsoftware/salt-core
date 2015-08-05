package com.uncharted.mosaic.util

import java.sql.{Date, Timestamp}
import org.apache.spark.sql.Row

/**
 * Common operations for DataFrames which compose our source data, such as
 * the retrieval of sample rows or the computation of summary statistics.
 */
object DataFrameUtil {
  def getDouble(col: Int, r: Row): Double = {
    r.get(col) match {
      case b: Byte => b.toDouble
      case s: Short => s.toDouble
      case i: Int => i.toDouble
      case l: Long => l.toDouble
      case f: Float => f.toDouble
      case d: Double => d
      case d: Date => (d.getTime).toDouble
      case t: Timestamp => (t.getTime).toDouble
      case _ => {
        val colType = r.get(col).getClass.getName
        throw new IllegalArgumentException(s"Column $col is non-numeric (type: $colType)")
      }
    }
  }
}
