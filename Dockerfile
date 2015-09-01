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

# download scalatest
RUN curl http://central.maven.org/maven2/org/scalatest/scalatest_2.10/2.2.5/scalatest_2.10-2.2.5.jar > /opt/libs/scalatest_2.10-2.2.5.jar

# download scoverage
RUN curl https://repo1.maven.org/maven2/org/scoverage/scalac-scoverage-runtime_2.10/1.1.1/scalac-scoverage-runtime_2.10-1.1.1.jar > /opt/libs/scalac-scoverage-runtime_2.10-1.1.1.jar

# build app
RUN ./gradlew clean jarScoverage testJar

ENV MOSAIC_VERSION 0.0.1

CMD ["spark-submit", "--jars", "/opt/libs/scalatest_2.10-2.2.5.jar,/opt/libs/scalac-scoverage-runtime_2.10-1.1.1.jar,/opt/mosaic/build/libs/mosaic-$MOSAIC_VERSION-scoverage.jar", "--class", "com.unchartedsoftware.mosaic.Main", "build/libs/mosaic-$MOSAIC_VERSION-tests.jar"]
