#!/bin/sh

docker run \
-p 8080:8080 \
-p 9999:9999 \
-e GRADLE_OPTS="-Dorg.gradle.native=false" \
-v /$(pwd)/src/test/resources/log4j.properties:/usr/local/spark/conf/log4j.properties \
-v /$(pwd):/opt/salt \
-it \
--workdir="//opt/salt" \
uncharted/sparklet:1.6.0 bash
