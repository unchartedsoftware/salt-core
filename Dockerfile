#
# Mosaic Test Container
# Apache Spark 1.4.1
#
# Runs the Mosaic test suite in a container
#
# Usage:
# docker build -t docker.uncharted.software/mosaic-test .
# docker run --rm docker.uncharted.software/mosaic-test

FROM sequenceiq/spark:1.4.0
MAINTAINER Sean McIntyre <smcintyre@uncharted.software>

ADD . /opt/mosaic

WORKDIR /opt/mosaic
RUN mkdir /opt/libs
RUN curl http://central.maven.org/maven2/org/scalatest/scalatest_2.10/2.2.5/scalatest_2.10-2.2.5.jar > /opt/libs/scalatest_2.10-2.2.5.jar

# build app
RUN ./gradlew clean assemble testJar

ENV MOSAIC_VERSION 0.0.1

CMD ["spark-submit", "--jars", "/opt/libs/scalatest_2.10-2.2.5.jar,/opt/mosaic/build/libs/mosaic-$MOSAIC_VERSION.jar", "--class", "com.unchartedsoftware.mosaic.Main", "build/libs/mosaic-$MOSAIC_VERSION-tests.jar"]
