#!/bin/bash

# be verbose and exit on any non-zero exit code
# see https://docs.travis-ci.com/user/customizing-the-build/#Implementing-Complex-Build-Steps
set -ev

# set defaults if not already set
PRINT_SUMMARY=${PRINT_SUMMARY:-true}
RUN_ORDER=${RUN_ORDER:-alphabetical}
HEADLESS=${HEADLESS:-false}

export MAVEN_OPTS=-Xmx1536m


# run full GUI test suite and fail on coverage issues
mvn verify -U -P travis-coverage --batch-mode \
        -Dsurefire.printSummary=${PRINT_SUMMARY} \
        -Dsurefire.runOrder=${RUN_ORDER} \
        -Dsurefire.forkCount="24" \
        -Dant.jvm.args="-Djava.awt.headless=${HEADLESS}" \
        -Djava.awt.headless=${HEADLESS} \
        -Dcucumber.options="--tags 'not @Ignore'"

