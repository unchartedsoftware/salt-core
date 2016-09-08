#!/bin/sh

WORKDIR=`pwd | rev | cut -d "/" -f1 | rev`

RED='\033[0;31m'
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
BLUE='\033[0;94m'
RESET='\033[0;00m'

create_test_environment() {
  printf "${GREEN}Creating${RESET} new Spark test environment (container: ${BLUE}${WORKDIR}${RESET})...\n"
  docker run \
  --name $WORKDIR \
  -p 8080:8080 \
  -p 9999:9999 \
  -d \
  -e GRADLE_OPTS="-Dorg.gradle.native=false" \
  -v /`pwd`/src/test/resources/log4j.properties:/usr/local/spark/conf/log4j.properties \
  -v /`pwd`:`pwd` \
  -it \
  --workdir="/`pwd`" \
  uncharted/sparklet:2.0.0 bash
}

run_test_environment() {
  printf "${GREEN}Resuming${RESET} existing Spark test environment (container: ${BLUE}${WORKDIR}${RESET})...\n"
  docker start $WORKDIR
}

stop_test_environment() {
  printf "${YELLOW}Stopping${RESET} Spark test environment (container: ${BLUE}${WORKDIR}${RESET})...\n"
  docker stop $WORKDIR
}

kill_test_environment() {
  printf "${RED}Destroying${RESET} Spark test environment (container: ${BLUE}${WORKDIR}${RESET})...\n"
  docker rm -fv $WORKDIR
}

verify_test_environment() {
  PRESENT=$(docker ps -a -q -f name=$WORKDIR)
  if [ -n "$PRESENT" ]; then
    run_test_environment
  else
    create_test_environment
  fi
}

if [ "$1" = "stop" ]; then
  stop_test_environment
elif [ "$1" = "rm" ]; then
  kill_test_environment
else
  verify_test_environment
fi
