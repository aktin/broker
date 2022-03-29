#!/usr/bin/env bash

HOST_AND_PORT=${HOST_AND_PORT:-"0.0.0.0:8080"}
PASSWORD=${PASSWORD:-"CHANGEME"}
LOG_LEVEL=${LOG_LEVEL:-INFO}

echo "Setting log level to $LOG_LEVEL"
sed -i 's/.level=.*/.level='$LOG_LEVEL'/g' logging.properties

"$JAVA_HOME"/bin/java -Daktin.broker.password=$PASSWORD -Djava.util.logging.config.file=logging.properties -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.JavaUtilLog -cp lib/\* org.aktin.broker.admin.standalone.HttpServer $HOST_AND_PORT 2>&1
