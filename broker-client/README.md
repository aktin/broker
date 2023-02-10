Broker Client library
=====================

The broker client library is not necessarily needed to connect to the broker server or use the broker server.
You can also use the RESTful API without using this client library.

For easy access of the broker API, this broker client library provides functionality
for administration and client access to the broker.

For live execution of requests via external process invocation, you can use the command line
interface provided via the class org.aktin.broker.client.live.sysproc.CLI.

Example to prepare standalone client execution:
```
# build using maven
mvn clean install
# go to target folder and copy jar together with dependencies
cd target
mv *.jar lib/
```

Run system process runner CLI client
```
# copy example configuration
cp ../src/test/resources/sysproc.properties ./
# before starting, edit sysproc.properties as needed
java -cp lib/\* org.aktin.broker.client.live.CLI org.aktin.broker.client.live.sysproc.ProcessExecutionPlugin sysproc.properties

```

For a simple example, we can also use the NoOp (no operation) execution plugin. The code of the Noop-Plugin can also serve as a template for custom execution plugins.
```
java -cp lib/\* org.aktin.broker.client.live.CLI org.aktin.broker.client.live.noop.NoopExecutionPlugin sysproc.properties
```



Run admin listener CLI client
```
java -cp lib/\* org.aktin.broker.client.live.util.AdminListener http://localhost:8080/broker/ org.aktin.broker.client2.auth.ApiKeyAuthentication xxxAdmin1234

```
