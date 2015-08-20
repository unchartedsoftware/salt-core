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
import com.uncharted.mosaic.generation.projection.{Projection, SpatialProjection}
import com.uncharted.mosaic.generation.TileGenerator
import com.uncharted.mosaic.generation.analytic._
import com.uncharted.mosaic.generation.analytic.numeric._
import com.uncharted.mosaic.generation.serialization.PrimitiveTypeAvroSerializer
import com.uncharted.mosaic.util.DataFrameUtil
import org.apache.spark.sql.Row

// source DataFrame
// NOTE: It is STRONGLY recommended that you filter your input DataFrame down to only the columns you need for tiling.
val frame = sqlContext.sql("select pickup_time, distance from taxi_micro")
frame.cache

// which tiles are we generating?
val indices = List((0,0,0), (1,0,0))
// max/min zoom
val zoomBounds = indices.reduceLeft((x:(Int, Int, Int), y:(Int, Int, Int)) => (x._1 min y._1, x._1 max y._1, 0))

// create a projection into 2D space using column 0 (pickup_time) and column 1 (distance), and appropriate max/min bounds for both.
val proj: Projection = new SpatialProjection(256, 256, zoomBounds._1, zoomBounds._2, 0, 1358725677000D, 1356998880000D, 1, 95.85D, 0)

// our value extractor does nothing, since we're just counting records
val extractor = new ValueExtractor[Double] {
  override def rowToValue(r: Row): Option[Double] = {
    return None
  }
}

// Tile Generator, with appropriate input, intermediate and output types for bin and tile aggregators (CountAggregator and MaxMinAggregator, in this case)
val gen = new TileGenerator[Double, Double, java.lang.Double, (Double, Double), (java.lang.Double, java.lang.Double)](sc, proj, extractor, CountAggregator, MaxMinAggregator)

// For serializing tiles to AVRO
val serializer = new PrimitiveTypeAvroSerializer[java.lang.Double, (java.lang.Double, java.lang.Double)](classOf[java.lang.Double], proj.xBins*proj.yBins)

// Flip the switch
val result = gen.generate(frame, tilesBuffer.toList)
result.map(t => (t._1, serializer.serialize(t._2)))
```

# Mosaic Library Contents

## Projections

Mosaic currently supports two projections:
 * SpatialProjection (x, y, v)
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
