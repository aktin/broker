Broker Client library
=====================

The broker client library is not necessarily needed to connect to the broker server or use the broker server.
You can also use the RESTful API without using this client library.

For easy access of the broker API, this broker client library provides functionality
for administration and client access to the broker.

For live execution of requests via external process invocation, you can use the command line
interface provided via the class org.aktin.broker.client.live.sysproc.CLI.

Example usage:
```
# build using maven
mvn clean install
# copy example configuration
cp src/test/resources/sysproc.properties target/
# go to target folder and run client
cd target
mv *.jar lib/
# before starting, edit sysproc.properties as needed
java -cp lib/\* org.aktin.broker.client.live.sysproc.CLI sysproc.properties

```