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

## Data

We need something to tile. Let's start with a small sample of the [NYC Taxi Dataset](http://www.andresmh.com/nyctaxitrips/), which can be [downloaded from here](http://assets.oculusinfo.com/pantera/taxi_micro.csv).

Create a temp table called "taxi_micro" with the following schema:

```scala
scala> sqlContext.sql("select * from taxi_micro").schema

res5: org.apache.spark.sql.types.StructType = StructType(StructField(hack,StringType,true), StructField(license,StringType,true), StructField(code,StringType,true), StructField(flag,IntegerType,true), StructField(type,StringType,true), StructField(pickup_time,TimestampType,true), StructField(dropoff_time,TimestampType,true), StructField(passengers,IntegerType,true), StructField(duration,IntegerType,true), StructField(distance,DoubleType,true), StructField(pickup_lon,DoubleType,true), StructField(pickup_lat,DoubleType,true), StructField(dropoff_lon,DoubleType,true), StructField(dropoff_lat,DoubleType,true))
```

## Tile Generation

Let's generate tiles which represent the mean number of passengers at each pickup location in the source dataset.

```scala
import com.unchartedsoftware.mosaic.core.projection.numeric._
import com.unchartedsoftware.mosaic.core.generation.Series
import com.unchartedsoftware.mosaic.core.generation.mapreduce.MapReduceTileGenerator
import com.unchartedsoftware.mosaic.core.analytic._
import com.unchartedsoftware.mosaic.core.generation.request._
import com.unchartedsoftware.mosaic.core.analytic.numeric._
import java.sql.Timestamp
import org.apache.spark.sql.Row

// source RDD
// NOTE: It is STRONGLY recommended that you filter your input RDD down to only the columns you need for tiling.
val rdd = sqlContext.sql("select pickup_lon, pickup_lat, passengers from taxi_micro").rdd
rdd.cache

// We use a value extractor function to retrieve data-space coordinates from rows
// In this case, that's column 0 (pickup_time, converted to a double millisecond value) and column 1 (distance)
val cExtractor = (r: Row) => {
  if (r.isNullAt(0) || r.isNullAt(1)) {
    None
  } else {
    Some(r.getDouble(0), r.getDouble(1)))
  }
}

// create a projection from data-space into mercator tile space, which is suitable for
// display on top of a map using a mapping library such as leaflet.js
val projection = new MercatorProjection()

// a value extractor function to grab the number of passengers from a Row
val vExtractor = (r: Row) => {
  if (r.isNullAt(2)) {
    None
  } else {
    Some(r.getInt(2).toDouble)
  }
}

// A series ties the value extractors, projection and bin/tile aggregators together.
// We'll be tiling average passengers per bin, and max/min of the bin averages per tile
// We'll also divide our tiles into 32x32 bins so that the output is readable. We specify
// this using the maximum possible bin index, which is (31,31)
val series = new Series((31, 31), cExtractor, projection, Some(vExtractor), MeanAggregator, Some(MinMaxAggregator))

// which tiles are we generating? In this case, we'll use a TileSeqRequest
// which allows us to specify a list of tiles we're interested in, by coordinate.
// We also have to supply this particular request type with a mechanism for
// extracting zoom levels from a tile coordinate, as the second construction parameter.
val request = new TileSeqRequest(Seq((0,0,0), (1,0,0)), (t: (Int, Int, Int)) => t._1)

// Tile Generator object, which houses the generation logic
@transient val gen = new MapReduceTileGenerator(sc)

// Flip the switch by passing in the series and the request
val result = gen.generate(rdd, Seq(series), request)

// Try to read some values from bins, from the first (and only) series
result.map(t => (t(0).coords, t(0).bins)).collect
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
 * MinMaxAggregator (for tile-level analytics)
 * MeanAggregator
 * SumAggregator
 * TopElementsAggregator

Additional aggregators can be implemented on-the-fly within your script as you see fit.

## Requests

Mosaic allows tile batches to be phrased in several ways:

 * TileSeqRequest (built from a Seq[TC] of tile coordinates, requesting specific tiles)
 * TileLevelRequest (built from a Seq[Int] of levels, requesting all tiles at those levels)

## Serialization

Mosaic currently supports serializing tiles consisting of basic type values to Apache Avro which is fully compliant with the aperture-tiles sparse/dense schemas. This functionality is provided in a separate package called mosaic-avro-serializer.
