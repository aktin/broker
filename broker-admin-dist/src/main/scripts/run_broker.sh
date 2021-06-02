#!/usr/bin/env bash

"$JAVA_HOME"/bin/java -Daktin.broker.password=CHANGEME -Djava.util.logging.config.file=logging.properties -cp lib/\* org.aktin.broker.admin.standalone.HttpServer 8080
