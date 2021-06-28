#!/usr/bin/env bash

echo "Running aktin client from console"
java $JAVA_OPTS -cp lib/\* org.aktin.broker.client.live.sysproc.CLI sysproc.properties
