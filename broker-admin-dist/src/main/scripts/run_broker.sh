#!/usr/bin/env bash

"$JAVA_HOME"/bin/java -Daktin.broker.password=CHANGEME -Djava.util.logging.config.file=logging.properties -cp lib/\* org.aktin.broker.admin.standalone.HttpServer 8080

# You can override the JDBC url by specifying -Daktin.broker.jdbc.url=jdbc:hsqldb:file:./broker\;shutdown=false\;user=admin\;password=secret

# To use a different database e.g. postgres, copy its JDBC driver to the lib folder and use the system properties aktin.broker.jdbc.url and -Daktin.broker.jdbc.datasource.class
