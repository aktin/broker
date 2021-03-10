Use the broker server
=====================

To use the broker server, run a JAX-RS server with
the endpoints listed in org.aktin.broker.Broker

For running behind a reverse proxy, make sure to forward the following
http paths: `/broker/*` and `/aggregator/*`.

For proxying websocket connections, the following paths must be 
configured with protocol upgrade functionality: `/broker/websocket` and `broker/my/websocket`.

 


Authentication
==============

API-Key authentication
----------------------
API-keys can be specified for node access to REST API. See api-keys.properties in broker-admin or broker-admin-dist.


Use the REST API from the command line
--------------------------------------

```
curl -H 'Accept: application/xml' -H "Authorization: Bearer xxxApiKey123" https://localhost:8080/broker/my/node
```

View database contents
----------------------
The HSQL database can be viewed with the command
```
java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.4/hsqldb-2.3.4.jar org.hsqldb.util.DatabaseManager

```
 