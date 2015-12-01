#!/bin/sh

docker run \
-e GRADLE_OPTS="-Dorg.gradle.daemon=true" \
-v $(pwd)/src/test/resources/log4j.properties:/usr/local/spark/conf/log4j.properties \
-v $(pwd):/opt/salt \
-it \
--workdir="/opt/salt" \
uncharted/sparklet:1.5.2 bash
