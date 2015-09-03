package com.unchartedsoftware.mosaic.core.projection

import com.unchartedsoftware.mosaic.core.util.DataFrameUtil
import org.scalatest._
import org.apache.spark.sql.Row
import java.sql.{Date, Timestamp}

class DataFrameUtilSpec extends FunSpec {
  describe("DataFrameUtil") {
    describe("#toDouble()") {
      it("should extract Doubles from Rows without transformation") {
        val row = Row(12D, 10D)
        assert(DataFrameUtil.getDouble(0, row) === 12D)
      }

      it("should extract Timestamps from Rows, converting them to a millisecond value") {
        val time = new Timestamp(System.currentTimeMillis)
        val row = Row(time)
        assert(DataFrameUtil.getDouble(0, row) === time.getTime.toDouble)
      }

      it("should extract Longs from Rows, converting them to Doubles") {
        val row = Row(12L)
        assert(DataFrameUtil.getDouble(0, row) === row.getLong(0).toDouble)
      }

      it("should extract Floats from Rows, converting them to Doubles") {
        val row = Row(12F)
        assert(DataFrameUtil.getDouble(0, row) === row.getFloat(0).toDouble)
      }

      it("should extract Ints from Rows, converting them to Doubles") {
        val row = Row(12)
        assert(DataFrameUtil.getDouble(0, row) === row.getInt(0).toDouble)
      }

      it("should extract Shorts from Rows, converting them to Doubles") {
        val row = Row(12.toShort)
        assert(DataFrameUtil.getDouble(0, row) === row.getShort(0).toDouble)
      }

      it("should extract Dates from Rows, converting them to a millisecond value") {
        val time = new Date(System.currentTimeMillis)
        val row = Row(time)
        assert(DataFrameUtil.getDouble(0, row) === time.getTime.toDouble)
      }

      it("should extract Bytes from Rows, converting them to Doubles") {
        val row = Row(8.toByte)
        assert(DataFrameUtil.getDouble(0, row) === row.getByte(0).toDouble)
      }

      it("should throw an IllegalArgumentException for non-numeric columns") {
        val row = Row("hello world")
        intercept[IllegalArgumentException] {
          DataFrameUtil.getDouble(0, row)
        }
      }
    }
  }
}
