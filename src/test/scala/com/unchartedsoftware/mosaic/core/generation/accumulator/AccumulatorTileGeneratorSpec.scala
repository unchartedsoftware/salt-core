package com.unchartedsoftware.mosaic.core.analytic.numeric

import org.scalatest._
import com.unchartedsoftware.mosaic.Spark
import com.unchartedsoftware.mosaic.core.projection.Projection
import com.unchartedsoftware.mosaic.core.projection.numeric._
import com.unchartedsoftware.mosaic.core.generation.accumulator.AccumulatorTileGenerator
import com.unchartedsoftware.mosaic.core.generation.output._
import com.unchartedsoftware.mosaic.core.generation.request._
import com.unchartedsoftware.mosaic.core.analytic._
import com.unchartedsoftware.mosaic.core.analytic.numeric._
import com.unchartedsoftware.mosaic.core.util.ValueExtractor
import org.apache.spark.sql.Row

//define tests here so that scalatest stuff isn't serialized into spark closures
object AccumulatorTileGeneratorSpecClosure {

  def testClosure(
    data: Array[Double],
    projection: Projection[(Int, Int)],
    request: TileSeqRequest[(Int, Int)],
    vExtractor: ValueExtractor[Double]
  ): Seq[TileData[(Int, Int), java.lang.Double, (java.lang.Double, java.lang.Double)]] = {
    //generate some random data
    var frame = Spark.sc.parallelize(data.map(a => Row(a)))

    //create generator
    val gen = new AccumulatorTileGenerator[(Int, Int), Double, Double, java.lang.Double, (Double, Double), (java.lang.Double, java.lang.Double)](Spark.sc, projection, vExtractor, CountAggregator, MaxMinAggregator)

    //kickoff generation
    gen.generate(frame, request)
  }
}

class AccumulatorTileGeneratorSpec extends FunSpec {
  describe("AccumulatorTileGenerator") {
    describe("#generate()") {
      it("should generate tile level 0, correctly distributing input points into bins") {

        //generate some random data
        val data = Array.fill(10)(0D).map(a => Math.random)

        //manually bin
        val manualBins = data.groupBy(a => a > 0.5).map(a => (a._1, a._2.length))

        //create projection, request, extractors
        val dataSpaceExtractor = new ValueExtractor[Double] {
          override def rowToValue(r: Row): Option[Double] = {
            return Some(r.getDouble(0))
          }
        }
        val projection = new SeriesProjection(2, 0, 1, dataSpaceExtractor, 0D, 1D)
        val request = new TileSeqRequest[(Int, Int)](Seq((0,0)), projection)
        val vExtractor = new ValueExtractor[Double] {
          override def rowToValue(r: Row): Option[Double] = {
            return None
          }
        }

        val tiles = AccumulatorTileGeneratorSpecClosure.testClosure(data, projection, request, vExtractor)
        assert(tiles.length === 1) //did we generate a tile?

        //verify binning
        assert(tiles(0).bins(0) === manualBins.get(false).getOrElse(0))
        assert(tiles(0).bins(1) === manualBins.get(true).getOrElse(0))

        //verify max/min tile analytic
        val min = tiles(0).bins.reduce((a,b) => Math.min(a, b))
        val max = tiles(0).bins.reduce((a,b) => Math.max(a, b))
        assert(tiles(0).tileMeta._1 === min)
        assert(tiles(0).tileMeta._2 === max)

        //verify bins touched
        val binsTouched = manualBins.toSeq.length
        assert(tiles(0).binsTouched === binsTouched)
      }

      it("should ignore rows which are outside the bounds of the projection") {
        //generate some random data
        val data = Array.fill(10)(0D).map(a => Math.random)

        //manually bin
        val manualBins = data.filter(a => a <= 0.5).groupBy(a => a > 0.25).map(a => (a._1, a._2.length))

        //create projection, request, extractors
        val dataSpaceExtractor = new ValueExtractor[Double] {
          override def rowToValue(r: Row): Option[Double] = {
            return Some(r.getDouble(0))
          }
        }
        val projection = new SeriesProjection(2, 0, 1, dataSpaceExtractor, 0D, 0.5D)
        val request = new TileSeqRequest[(Int, Int)](Seq((0,0)), projection)
        val vExtractor = new ValueExtractor[Double] {
          override def rowToValue(r: Row): Option[Double] = {
            return None
          }
        }

        val tiles = AccumulatorTileGeneratorSpecClosure.testClosure(data, projection, request, vExtractor)
        assert(tiles.length === 1) //did we generate a tile?

        //verify binning
        assert(tiles(0).bins(0) === manualBins.get(false).getOrElse(0))
        assert(tiles(0).bins(1) === manualBins.get(true).getOrElse(0))

        //verify max/min tile analytic
        val min = tiles(0).bins.reduce((a,b) => Math.min(a, b))
        val max = tiles(0).bins.reduce((a,b) => Math.max(a, b))
        assert(tiles(0).tileMeta._1 === min)
        assert(tiles(0).tileMeta._2 === max)

        //verify bins touched
        val binsTouched = manualBins.toSeq.length
        assert(tiles(0).binsTouched === binsTouched)
      }

      it("should generate successive tile levels, correctly distributing input points into bins") {

        //generate some random data
        val data = Array.fill(10)(0D).map(a => Math.random)

        //create projection, request, extractors
        val dataSpaceExtractor = new ValueExtractor[Double] {
          override def rowToValue(r: Row): Option[Double] = {
            return Some(r.getDouble(0))
          }
        }
        val projection = new SeriesProjection(10, 0, 1, dataSpaceExtractor, 0D, 1D)
        val request = new TileSeqRequest[(Int, Int)](Seq((0,0), (1,0), (1,1)), projection)
        val vExtractor = new ValueExtractor[Double] {
          override def rowToValue(r: Row): Option[Double] = {
            return None
          }
        }

        val tiles = AccumulatorTileGeneratorSpecClosure.testClosure(data, projection, request, vExtractor)
        assert(tiles.length === 3) //did we generate tiles?

        //map the result so that it's easier to work with
        val tilesMap = tiles.map(a => (a.coords, a)).toMap

        //verify binning of level 1 by aggregating it into level 0
        val combinedOneBins = tilesMap.get((1,0)).get.bins ++ tilesMap.get((1,1)).get.bins

        //verify tile levels 1 and 0 are consistent
        var j = 0
        for (i <- 0 until 10) {
          val zeroBin = tilesMap.get((0,0)).get.bins(i)
          assert(zeroBin === combinedOneBins(j) + combinedOneBins(j+1))
          j = j + 2
        }
      }
    }
  }
}
