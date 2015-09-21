# Mosaic
> Smaller tiles.

# Building/Installing

```
$ ./gradlew jar
$ ./gradlew install
```

# Testing

Since testing Mosaic requires a Spark cluster, a containerized test environment is included via [Docker](https://www.docker.com/). If you have docker installed, you can build and test Mosaic within that environment:

```bash
$ docker build -t docker.uncharted.software/mosaic-test .
$ docker run --rm docker.uncharted.software/mosaic-test
```

The above commands trigger a one-off build and test of Mosaic. If you want to interactively test Mosaic while developing (without having to re-run the container), use the following commands:

```bash
$ docker build -t docker.uncharted.software/mosaic-test .
$ docker run -v $(pwd):/opt/mosaic -it docker.uncharted.software/mosaic-test bash
# then, inside the running container
$ ./gradlew
```

This will mount the code directory into the container as a volume, allowing you to make code changes on your host machine and test them on-the-fly.

# Tiling

We need something to tile. Let's start with a small sample of the [NYC Taxi Dataset](http://www.andresmh.com/nyctaxitrips/), which can be [downloaded from here](http://assets.oculusinfo.com/pantera/taxi_micro.csv).

Create a temp table called "taxi_micro" with the following schema:

```scala
scala> sqlContext.sql("select * from taxi_micro").schema

res5: org.apache.spark.sql.types.StructType = StructType(StructField(hack,StringType,true), StructField(license,StringType,true), StructField(code,StringType,true), StructField(flag,IntegerType,true), StructField(type,StringType,true), StructField(pickup_time,TimestampType,true), StructField(dropoff_time,TimestampType,true), StructField(passengers,IntegerType,true), StructField(duration,IntegerType,true), StructField(distance,DoubleType,true), StructField(pickup_lon,DoubleType,true), StructField(pickup_lat,DoubleType,true), StructField(dropoff_lon,DoubleType,true), StructField(dropoff_lat,DoubleType,true))
```

## Tiling with Accumulators

We can generate 2D, non-geo tiles for count() at pickup_time x distance as follows:

```scala
import com.unchartedsoftware.mosaic.core.projection._
import com.unchartedsoftware.mosaic.core.generation.accumulator.AccumulatorTileGenerator
import com.unchartedsoftware.mosaic.core.analytic._
import com.unchartedsoftware.mosaic.core.generation.request._
import com.unchartedsoftware.mosaic.core.analytic.numeric._
import org.apache.spark.sql.Row

// source DataFrame
// NOTE: It is STRONGLY recommended that you filter your input DataFrame down to only the columns you need for tiling.
val frame = sqlContext.sql("select pickup_time, distance from taxi_micro").rdd
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

// Flip the switch
val result = gen.generate(frame, request)

// Try to read some values from bins
result.map(t => (t.coords, t.bins))
```

## Tiling with Map/Reduce

This process is almost identical to accumulator tile generation, but with a slightly different final step since the generated tiles are distributed in an RDD instead of being shipped back to the Spark master.

```scala
import com.unchartedsoftware.mosaic.core.projection._
import com.unchartedsoftware.mosaic.core.generation.mapreduce.MapReduceTileGenerator
import com.unchartedsoftware.mosaic.core.analytic._
import com.unchartedsoftware.mosaic.core.generation.request._
import com.unchartedsoftware.mosaic.core.analytic.numeric._
import org.apache.spark.sql.Row

// source DataFrame
// NOTE: It is STRONGLY recommended that you filter your input DataFrame down to only the columns you need for tiling.
val frame = sqlContext.sql("select pickup_time, distance from taxi_micro").rdd
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

// Try to read some values from bins
result.map(t => (t.coords, t.bins)).collect
```

# Mosaic Library Contents

## Projections

Mosaic currently supports three projections:
 * CartesianProjection (x, y, v)
 * MercatorProjection (x, y, v)
 * SeriesProjection (x, v)

## Aggregators

Mosaic includes seven sample aggregators:

 * CountAggregator
 * MaxAggregator
 * MinAggregator
 * MaxMinAggregator (for tile-level analytics)
 * MeanAggregator
 * SumAggregator
 * TopElementsAggregator

Additional aggregators can be implemented on-the-fly within your script as you see fit.

## Requests

Mosaic allows tile batches to be phrased in several ways:

 * TileSeqRequest (built from a Seq[TC] of tile coordinates, requesting specific tiles)
 * TileLevelRequest (built from a Seq[Int] of levels, requesting all tiles at those levels)

## Generation

Mosaic supports two strategies for tile generation:

 * **ActiveTileGenerator**: Produces a Seq[TileData]. In contrast with LazyTileGenerator, these tiles actually exist when generate() returns, since they have been pulled back to the Spark driver generate() returns (possibly because the driver needed to manipulate them somehow in a non-distributed fashion).

 * **LazyTileGenerator**: Produces an RDD[TileData] which only materializes when an operation pulls some or all of those tiles back to the Spark driver

## Serialization

Mosaic currently supports serializing tiles consisting of basic type values to Apache Avro which is fully compliant with the aperture-tiles sparse/dense schemas. This functionality is provided in a separate package called mosaic-avro-serializer.
