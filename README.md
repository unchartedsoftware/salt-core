# mosaic
> Smaller tiles.

# Building

```
$ ./gradlew build
```

# Usage

Assuming you have a table such as:

```scala
scala> sqlContext.sql("select * from taxi_micro").schema

res5: org.apache.spark.sql.types.StructType = StructType(StructField(hack,StringType,true), StructField(license,StringType,true), StructField(code,StringType,true), StructField(flag,IntegerType,true), StructField(type,StringType,true), StructField(pickup_time,TimestampType,true), StructField(dropoff_time,TimestampType,true), StructField(passengers,IntegerType,true), StructField(duration,IntegerType,true), StructField(distance,DoubleType,true), StructField(pickup_lon,DoubleType,true), StructField(pickup_lat,DoubleType,true), StructField(dropoff_lon,DoubleType,true), StructField(dropoff_lat,DoubleType,true))
```

Then you can generate 2D, non-geo tiles for count() at pickup_time x distance as follows:

```scala
import com.unchartedsoftware.mosaic.core.projection._
import com.unchartedsoftware.mosaic.core.generation.accumulator.AccumulatorTileGenerator
import com.unchartedsoftware.mosaic.core.analytic._
import com.unchartedsoftware.mosaic.core.analytic.numeric._
import com.unchartedsoftware.mosaic.core.serialization.PrimitiveTypeAvroSerializer
import com.unchartedsoftware.mosaic.core.util.DataFrameUtil
import org.apache.spark.sql.Row

// source DataFrame
// NOTE: It is STRONGLY recommended that you filter your input DataFrame down to only the columns you need for tiling.
val frame = sqlContext.sql("select pickup_time, distance from taxi_micro")
frame.cache

// which tiles are we generating?
// we use (Int, Int, Int) to represent them, since this is the coordinate type for CartesianProjection (which we'll employ below)
val indices = List((0,0,0), (1,0,0))

// max/min zoom
val zoomBounds = indices.foldLeft((Int.MaxValue, Int.MinValue))((c:(Int, Int), next:(Int, Int, Int)) => (c._1 min next._1, c._2 max next._1))

// create a projection into 2D space using column 0 (pickup_time) and column 1 (distance), and appropriate max/min bounds for both.
val proj = new CartesianProjection(256, 256, zoomBounds._1, zoomBounds._2, 0, 1358725677000D, 1356998880000D, 1, 95.85D, 0)

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
val result = gen.generate(frame, indices)
result.map(t => (t._1, serializer.serialize(t._2)))
```

# Mosaic Library Contents

## Projections

Mosaic currently supports two projections:
 * CartesianProjection (x, y, v)
 * SeriesProjection (x, v)

A mercator projection would be nice, but hasn't been implemented yet.

## Aggregators

Mosaic includes six sample aggregators:

 * CountAggregator
 * MaxAggregator
 * MinAggregator
 * MaxMinAggregator (for tile-level analytics)
 * MeanAggregator
 * SumAggregator

Additional aggregators can be implemented on-the-fly within your script as you see fit.

## Generation

Mosiac supports live tiling, but does not currently include an intuitive batch mode.

A poor man's batch mode could be implemented as follows, but would probably run out of memory unless the long tile list was broken up into batches.

```scala
import scala.collection.mutable.ListBuffer

val zoomLevel = 17
val tilesBuffer = new ListBuffer[(Int, Int, Int)]()
for (z <- 0 until zoomLevel+1) {
  for (x <- 0 until Math.pow(2,z).toInt) {
    for (y <- 0 until Math.pow(2,z).toInt) {
      tilesBuffer.append((z, x, y))
    }
  }
}
val indices = tilesBuffer.toList
```

## Serialization

Mosaic currently supports serializing tiles consisting of basic type values to Apache Avro which is fully compliant with the aperture-tiles sparse/dense schemas.
