# Mosaic
> Smaller tiles.

# Building

```
$ ./gradlew build
```

# Getting Started

We need something to tile. Let's start with a small sample of the [NYC Taxi Dataset](http://www.andresmh.com/nyctaxitrips/), which can be [downloaded from here](http://assets.oculusinfo.com/pantera/taxi_micro.csv).

Create a temp table called "taxi_micro" with the following schema:

```scala
scala> sqlContext.sql("select * from taxi_micro").schema

res5: org.apache.spark.sql.types.StructType = StructType(StructField(hack,StringType,true), StructField(license,StringType,true), StructField(code,StringType,true), StructField(flag,IntegerType,true), StructField(type,StringType,true), StructField(pickup_time,TimestampType,true), StructField(dropoff_time,TimestampType,true), StructField(passengers,IntegerType,true), StructField(duration,IntegerType,true), StructField(distance,DoubleType,true), StructField(pickup_lon,DoubleType,true), StructField(pickup_lat,DoubleType,true), StructField(dropoff_lon,DoubleType,true), StructField(dropoff_lat,DoubleType,true))
```

# Tiling with Accumulators

We can generate 2D, non-geo tiles for count() at pickup_time x distance as follows:

```scala
import com.unchartedsoftware.mosaic.core.projection._
import com.unchartedsoftware.mosaic.core.generation.accumulator.AccumulatorTileGenerator
import com.unchartedsoftware.mosaic.core.analytic._
import com.unchartedsoftware.mosaic.core.generation.request._
import com.unchartedsoftware.mosaic.core.analytic.numeric._
import com.unchartedsoftware.mosaic.core.serialization.PrimitiveTypeAvroSerializer
import com.unchartedsoftware.mosaic.core.util.DataFrameUtil
import org.apache.spark.sql.Row

// source DataFrame
// NOTE: It is STRONGLY recommended that you filter your input DataFrame down to only the columns you need for tiling.
val frame = sqlContext.sql("select pickup_time, distance from taxi_micro")
frame.cache

// create a projection into 2D space using column 0 (pickup_time) and column 1 (distance), and appropriate max/min bounds for both.
val proj = new CartesianProjection(256, 256, 0, 1, 0, 1358725677000D, 1356998880000D, 1, 95.85D, 0)

// which tiles are we generating?
val request = new TileSeqRequest(Seq((0,0,0), (1,0,1)), proj)

// our value extractor does nothing, since we're just counting records
val extractor = new ValueExtractor[Double] {
  override def rowToValue(r: Row): Option[Double] = {
    return None
  }
}

// Tile Generator, with appropriate coord, input, intermediate and output types for bin and tile aggregators (CountAggregator and MaxMinAggregator, in this case)
val gen = new AccumulatorTileGenerator[(Int, Int, Int), Double, Double, java.lang.Double, (Double, Double), (java.lang.Double, java.lang.Double)](sc, proj, extractor, CountAggregator, MaxMinAggregator)

// For serializing basic spatial and series tiles to AVRO
val serializer = new PrimitiveTypeAvroSerializer[java.lang.Double, (java.lang.Double, java.lang.Double)](classOf[java.lang.Double], proj.bins)

// Flip the switch
val result = gen.generate(frame, request)
result.map(t => (t.coords, serializer.serialize(t)))
```

# Tiling with Map/Reduce

This process is almost identical to accumulator tile generation, but with a slightly different serialization step since the generated tiles are distributed in an RDD instead of being shipped back to the spark master.

```scala
import com.unchartedsoftware.mosaic.core.projection._
import com.unchartedsoftware.mosaic.core.generation.mapreduce.MapReduceTileGenerator
import com.unchartedsoftware.mosaic.core.analytic._
import com.unchartedsoftware.mosaic.core.generation.request._
import com.unchartedsoftware.mosaic.core.analytic.numeric._
import com.unchartedsoftware.mosaic.core.serialization.PrimitiveTypeAvroSerializer
import com.unchartedsoftware.mosaic.core.util.DataFrameUtil
import org.apache.spark.sql.Row

// source DataFrame
// NOTE: It is STRONGLY recommended that you filter your input DataFrame down to only the columns you need for tiling.
val frame = sqlContext.sql("select pickup_time, distance from taxi_micro")
frame.cache

// create a projection into 2D space using column 0 (pickup_time) and column 1 (distance), and appropriate max/min bounds for both.
val proj = new CartesianProjection(256, 256, 0, 1, 0, 1358725677000D, 1356998880000D, 1, 95.85D, 0)

// which tiles are we generating?
val request = new TileSeqRequest(Seq((0,0,0), (1,0,0)), proj)

// our value extractor does nothing, since we're just counting records
val extractor = new ValueExtractor[Double] {
  override def rowToValue(r: Row): Option[Double] = {
    return None
  }
}

// Tile Generator, with appropriate coord, input, intermediate and output types for bin and tile aggregators (CountAggregator and MaxMinAggregator, in this case)
@transient val gen = new MapReduceTileGenerator[(Int, Int, Int), Double, Double, java.lang.Double, (Double, Double), (java.lang.Double, java.lang.Double)](sc, proj, extractor, CountAggregator, MaxMinAggregator)

// Flip the switch
val result = gen.generate(frame, request)
result.mapPartitions(p => {  
  // We make one serializer per partition, since the output of this process is an RDD and we can't just keep one on the master.
  val s = new PrimitiveTypeAvroSerializer[java.lang.Double, (java.lang.Double, java.lang.Double)](classOf[java.lang.Double], proj.bins)
  p.map(t => {
    s.serialize(t)
  })
}).collect
```

# Mosaic Library Contents

## Projections

Mosaic currently supports three projections:
 * CartesianProjection (x, y, v)
 * MercatorProjection (x, y, v)
 * SeriesProjection (x, v)

## Aggregators

Mosaic includes six sample aggregators:

 * CountAggregator
 * MaxAggregator
 * MinAggregator
 * MaxMinAggregator (for tile-level analytics)
 * MeanAggregator
 * SumAggregator

Additional aggregators can be implemented on-the-fly within your script as you see fit.

## Requests

Mosaic allows tile batches to be phrased in several ways:

 * TileSeqRequest (built from a Seq[TC] of tile coordinates, requesting specific tiles)
 * TileLevelRequest (built from a Seq[Int] of levels, requesting all tiles at those levels)

## Generation

Mosaic supports two strategies for tile generation:

### OnDemandTileGenerator

TODO

### BatchTileGenerator

TODO

## Serialization

Mosaic currently supports serializing tiles consisting of basic type values to Apache Avro which is fully compliant with the aperture-tiles sparse/dense schemas.
