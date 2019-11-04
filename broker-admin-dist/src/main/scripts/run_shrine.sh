#!/usr/bin/env bash

java -Daktin.broker.password=CHANGEME -Djava.util.logging.config.file=logging.properties -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.JavaUtilLog -cp lib/\* org.aktin.broker.admin.standalone.HttpServer 8080
