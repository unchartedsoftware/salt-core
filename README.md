# Salt
> Looking at big data? Add a little salt.

## Usage

### Source data

We need something to tile. Let's start with a small sample of the [NYC Taxi Dataset](http://www.andresmh.com/nyctaxitrips/), which can be [downloaded from here](http://assets.oculusinfo.com/pantera/taxi_micro.csv).

### Tile generation, made easy!

Let's generate tiles which represent the mean number of passengers at each pickup location in the source dataset.

To begin, we'll need a spark-shell. If you have your own Spark cluster, skip ahead to [Generation](#example-generation). Otherwise, continue to the next step to fire up a small Spark test cluster via [Docker](https://www.docker.com/). You'll want at least 4GB of free RAM on your machine to use this latter method.

#### Using the Docker test container

Since running Salt requires a Spark cluster, a containerized test environment is included via [Docker](https://www.docker.com/). If you have docker installed, you can run the following example within that containerized environment.

Build and fire up the container with a shell:

```bash
$ docker build -t docker.uncharted.software/salt-test .
$ docker run -v $(pwd):/opt/salt -it docker.uncharted.software/salt-test bash
```

Now, inside the container, build and install Salt:

```bash
$ ./gradlew install
```

Keep the container running! We'll need it to try the following example.

#### <a name="example-generation"></a>Generation

Launch a spark-shell. We'll be using salt, and a popular csv->DataFrame library for this example:

```bash
$ spark-shell --packages "com.databricks:spark-csv_2.10:1.2.0,software.uncharted.salt:salt-core:0.14.0"
```

Now it's time to run a simple tiling job! Enter paste mode (:paste), and paste the following script:

```scala
import software.uncharted.salt.core.projection.numeric._
import software.uncharted.salt.core.generation.Series
import software.uncharted.salt.core.generation.mapreduce.MapReduceTileGenerator
import software.uncharted.salt.core.analytic._
import software.uncharted.salt.core.generation.request._
import software.uncharted.salt.core.analytic.numeric._
import java.sql.Timestamp
import org.apache.spark.sql.Row

// source RDD
// It is STRONGLY recommended that you filter your input RDD
// down to only the columns you need for tiling.
val rdd = sqlContext.read.format("com.databricks.spark.csv")
  .option("header", "true")
  .option("inferSchema", "true")
  .load("file:///taxi_micro.csv") // be sure to update the file path to reflect
                                  // the download location of taxi_micro.csv
  .select("pickup_lon", "pickup_lat", "passengers")
  .rdd

// cache the RDD to make things a bit faster
rdd.cache

// We use a value extractor function to retrieve data-space coordinates from rows
// In this case, that's column 0 (pickup_time, converted to a double millisecond value) and column 1 (distance)
val cExtractor = (r: Row) => {
  if (r.isNullAt(0) || r.isNullAt(1)) {
    None
  } else {
    Some((r.getDouble(0), r.getDouble(1)))
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

## Salt Library Contents

Salt is made of some simple, but vital, components:

### Projections

A projection maps from data space to the tile coordinate space.

Salt currently supports three projections:
 * CartesianProjection (x, y, v)
 * MercatorProjection (x, y, v)
 * SeriesProjection (x, v)

### Aggregators

Aggregators are used to aggregate values within a bin or a tile.

Salt includes seven sample aggregators:

 * CountAggregator
 * MaxAggregator
 * MinAggregator
 * MinMaxAggregator (for tile-level analytics)
 * MeanAggregator
 * SumAggregator
 * TopElementsAggregator

Additional aggregators can be implemented on-the-fly within your script as you see fit.

### Requests

Salt allows tile batches to be phrased in several ways:

 * TileSeqRequest (built from a Seq[TC] of tile coordinates, requesting specific tiles)
 * TileLevelRequest (built from a Seq[Int] of levels, requesting all tiles at those levels)

### Series

A Series pairs together a Projection with Aggregators. Multiple Series can be generated simultaneously, each operating on the source data in tandem.

### Serialization

Salt currently supports serializing tiles consisting of basic type values to Apache Avro which is fully compliant with the aperture-tiles sparse/dense schemas. This functionality is provided in a separate package called salt-avro-serializer.

## Testing

Since testing Salt requires a Spark cluster, a containerized test environment is included via [Docker](https://www.docker.com/). If you have docker installed, you can build and test Salt within that environment:

```bash
$ docker build -t docker.uncharted.software/salt-test .
$ docker run --rm docker.uncharted.software/salt-test
```

The above commands trigger a one-off build and test of Salt. If you want to interactively test Salt while developing (without having to re-run the container), use the following commands:

```bash
$ docker build -t docker.uncharted.software/salt-test .
$ docker run -v $(pwd):/opt/salt -it docker.uncharted.software/salt-test bash
# then, inside the running container
$ ./gradlew
```

This will mount the code directory into the container as a volume, allowing you to make code changes on your host machine and test them on-the-fly.
