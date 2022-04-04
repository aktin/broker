#!/usr/bin/env bash

LOG_LEVEL=${LOG_LEVEL:-INFO}
echo "Setting log level to $LOG_LEVEL"
sed -i 's/.level=.*/.level='$LOG_LEVEL'/g' logging.properties

echo "Running aktin client from console..."

java $JAVA_OPTS -cp lib/\* -Djava.util.logging.config.file=logging.properties org.aktin.broker.client.live.sysproc.CLI sysproc.properties
