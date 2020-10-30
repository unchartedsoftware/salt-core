# Salt &nbsp;[![Build Status](https://travis-ci.org/unchartedsoftware/salt-core.svg?branch=main)](https://travis-ci.org/unchartedsoftware/salt-core) [![Coverage Status](https://coveralls.io/repos/github/unchartedsoftware/salt-core/badge.svg?branch=main)](https://coveralls.io/github/unchartedsoftware/salt-core?branch=main)
> http://uncharted.software/salt

## Getting Started

Please note that this README represents an unreleased, bleeding-edge version of Salt. For up-to-date stable docs, please navigate to the appropriate **release tag** or [visit the Salt website](http://uncharted.software/salt).

## Examples

Example projects can be found at [salt-examples](https://github.com/unchartedsoftware/salt-examples).

## Usage

### Source data

We need something to tile. Let's start with a small sample of the [NYC Taxi Dataset](http://www.andresmh.com/nyctaxitrips/), which can be [downloaded from here](http://assets.oculusinfo.com/pantera/taxi_micro.csv).

### Tile generation, made easy!

Let's generate tiles which represent the mean number of passengers at each pickup location in the source dataset.

To begin, we'll need a spark-shell. If you have your own Spark cluster, skip ahead to [Generation](#example-generation). Otherwise, continue to the next step to fire up a small Spark test cluster via [Docker](https://www.docker.com/). You'll want at least 4GB of free RAM on your machine to use this latter method.

#### Using the Docker test container

Running Salt requires a Spark cluster.

Start and attach to the container that was just created:
```bash
$ ./test-environment
$ ./test-environment attach
```

You can remove the container at any time by running:
```bash
$ ./test-environment rm
```

Keep the container running! We'll need it to try the following example.

#### <a name="example-generation"></a>Generation

Attach to the container using `./test-environment attach`. Then download the sample data file and launch a spark-shell with salt to try this example:

```bash
$ curl -OL http://assets.oculusinfo.com/pantera/taxi_micro.csv
$ spark-shell --packages "software.uncharted.salt:salt-core:4.1.0"
```

Now it's time to run a simple tiling job! Enter paste mode (:paste), and paste the following script:

```scala
import software.uncharted.salt.core.projection.numeric._
import software.uncharted.salt.core.generation.Series
import software.uncharted.salt.core.generation.TileGenerator
import software.uncharted.salt.core.analytic._
import software.uncharted.salt.core.generation.request._
import software.uncharted.salt.core.analytic.numeric._
import java.sql.Timestamp
import org.apache.spark.sql.Row

// source RDD
// It is STRONGLY recommended that you filter your input RDD
// down to only the columns you need for tiling.
val rdd = spark.read.format("csv")
  .option("header", "true")
  .option("inferSchema", "true")
  .load("taxi_micro.csv")
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
// we specify the zoom levels we intend to support using a Seq[Int]
val projection = new MercatorProjection(Seq(0,1))

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
// We'll also divide our tiles into 8x8 bins so that the output is readable. We specify
// this using the maximum possible bin index, which is (7,7)
val avgPassengers = new Series((7, 7), cExtractor, projection, vExtractor, MeanAggregator, Some(MinMaxAggregator))

// which tiles are we generating? In this case, we'll use a TileSeqRequest
// which allows us to specify a list of tiles we're interested in, by coordinate.
// these tiles should be within the bounds of the Projection we created earlier
val request = new TileSeqRequest(Seq((0,0,0), (1,0,0)))

// Tile Generator object, which houses the generation logic
@transient val gen = TileGenerator(sc)

// Flip the switch by passing in the series and the request
// Note: Multiple series can be run against the source data
// at the same time, so a Seq[Series] is also supported as
// the second argument to generate()
val result = gen.generate(rdd, avgPassengers, request)

// Try to read some values from bins, from the first (and only) series
println(result.map(t => (avgPassengers(t).get.coords, avgPassengers(t).get.bins)).collect.deep.mkString("\n"))
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
