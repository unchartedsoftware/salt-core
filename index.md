---
layout: default
---

# Salt
> Looking at big data? Add a little salt.

Displaying a massive dataset is challenging. Your visualization must summarize all the data at a high level and still be able to reveal localized trends among billions of data points. But structuring your data to meet one of these requirements often precludes the ability to fulfill the other. And in either case, processing the data can consume significant time and resources.

Salt leverages a cluster-computing framework to create hierarchical, tile-based projections of big datasets. Salt projections start from an aggregate global view of the data and span increasingly detailed views until reaching a "street-level" depiction of individual data points. The generation of each view is distributed across machine resources to optimize processing times. The resulting hierarchical data projections support high-fidelity, interactive visualizations at that enable analyses at every scale.

## Usage

To understand how Salt works, we'll work with a small sample of [NYC taxi trip metadata from 2013](http://www.andresmh.com/nyctaxitrips/). From the source data, we'll generate a set of tiles that represent the mean number of passengers at each pickup location in the city.

[Download the NYC taxi dataset sample](http://assets.oculusinfo.com/pantera/taxi_micro.csv).

### Setting up a cluster

Before you start, you need to set up a Spark cluster to distribute the processing and generation of the data across machine resources. This will allow you to the optimize otherwise expensive and time-consuming computations.

For convenience, we have provided a small Spark test cluster container that you can build with [Docker](https://www.docker.com/), a virtualization platform. After you install Docker, make sure you have at least 4 GB of free RAM on your machine.
**NOTE**: If you have your own Spark cluster, skip ahead to [Generating Tiles](#example-generation).

###### To build and configure the Docker test container

1. Build and start the Spark test cluster container:

	{% highlight bash %}
	$ docker build -t docker.uncharted.software/salt-test .
	$ docker run -v $(pwd):/opt/salt -it docker.uncharted.software/salt-test bash
	{% endhighlight %}

2. Inside the container, build and install Salt:

	{% highlight bash %}
	$ ./gradlew install
	{% endhighlight %}

3. Copy the [NYC taxi dataset sample (taxi_micro.csv)](http://assets.oculusinfo.com/pantera/taxi_micro.csv) to the root directory within the container.

**NOTE**: Keep the container running! You'll need it for the following example.

### <a name="example-generation"></a> Generating Tiles

Now you're ready to create a set of tiles that summarize the taxi data.

###### To generate tiles from the source data

1. Launch a spark-shell with Salt and a csv->DataFrame library:

	{% highlight bash %}
	$ spark-shell --packages "com.databricks:spark-csv_2.10:1.2.0,software.uncharted.salt:salt-core:0.14.0"
	{% endhighlight %}

2. Run a simple tiling job by entering paste mode (:paste) and copying in the following script:

	{% highlight scala %}
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

	// We use a value extractor function to retrieve data-space coordinates from rows.
	// In this case, our coordinates are in column 0 (pickup_time, converted to a double millisecond value) and column 1 (distance).
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
	// We'll also divide our tiles into 8x8 bins so that the output is readable. We specify
	// this using the maximum possible bin index, which is (7,7)
	val avgPassengers = new Series((7, 7), cExtractor, projection, Some(vExtractor), MeanAggregator, Some(MinMaxAggregator))

	// which tiles are we generating? In this case, we'll use a TileSeqRequest
	// which allows us to specify a list of tiles we're interested in, by coordinate.
	// We also have to supply this particular request type with a mechanism for
	// extracting zoom levels from a tile coordinate, as the second construction parameter.
	val request = new TileSeqRequest(Seq((0,0,0), (1,0,0)), (t: (Int, Int, Int)) => t._1)

	// Tile Generator object, which houses the generation logic
	@transient val gen = new MapReduceTileGenerator(sc)

	// Flip the switch by passing in the series and the request
	// Note: Multiple series can be run against the source data
	// at the same time, so a Seq[Series] is also supported as
	// the second argument to generate()
	val result = gen.generate(rdd, avgPassengers, request)

	// Try to read some values from bins, from the first (and only) series
	println(result.map(t => (avgPassengers(t).coords, avgPassengers(t).bins)).collect.deep.mkString("\n"))
	{% endhighlight %}

## Salt Library Contents

Salt is made of the following simple but vital components:

### Projections

A projection maps from data-space to the tile coordinate space. Salt currently supports three projections:

 * CartesianProjection (x, y, v)
 * MercatorProjection (x, y, v)
 * SeriesProjection (x, v)

### Aggregators

Aggregators aggregate values within a bin or a tile. Salt includes seven sample aggregators:

 * CountAggregator
 * MaxAggregator
 * MinAggregator
 * MinMaxAggregator (for tile-level analytics)
 * MeanAggregator
 * SumAggregator
 * TopElementsAggregator

Additional aggregators can be implemented on demand within your script.

### Requests

Salt allows tile batches to be phrased in several ways:

 * TileSeqRequest (built from a Seq[TC] of tile coordinates, requesting specific tiles)
 * TileLevelRequest (built from a Seq[Int] of levels, requesting all tiles at those levels)

### Series

A series pairs together a projection with aggregators. Multiple series can be generated simultaneously, each operating on the source data in tandem.
