#!/usr/bin/env bash

echo "Running aktin client from console"
java -cp lib/\* org.aktin.broker.client.live.sysproc.CLI sysproc.properties
