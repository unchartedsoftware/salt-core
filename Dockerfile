#
# Mosaic Test Container
# Apache Spark 1.4.1
#
# Runs the Mosaic test suite in a container
#
# Usage:
# docker build -t docker.uncharted.software/mosaic-test .
# docker run docker.uncharted.software/mosaic-test

FROM sequenceiq/spark:1.4.0
MAINTAINER Sean McIntyre <smcintyre@uncharted.software>

ADD . /opt/mosaic

WORKDIR /opt/mosaic
CMD ["./gradlew", "clean", "build"]
