#!/bin/sh

local=$(pwd)
workdir=/opt/salt

if [[ "$OSTYPE" == "msys" ]]; then
	local="/$local"
	workdir="/$workdir"
fi

docker run \
-p 8080:8080 \
-p 9999:9999 \
-e GRADLE_OPTS="-Dorg.gradle.daemon=true" \
-v "$local/src/test/resources/log4j.properties":/usr/local/spark/conf/log4j.properties \
-v "$local":/opt/salt \
-it \
--workdir="$workdir" \
uncharted/sparklet:1.5.2 bash
