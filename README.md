AKTIN Broker 
============
A content-agnostic data exchange middle-ware for federated data warehouses.

This software is not designed for standalone usage. Rather it is to be used within federated
data warehouse environments like e.g. research networks or multi-center clinical study registries 
in conjunction with local workflows and processing infrastructure.

The broker infrastructure contains two main components: The central component `broker-server` is used to publish information to be distributed. The local component `broker-client` is used by multiple parties to retrieve the published information and subsequently report status updates and response-data.

The middleware infrastructure is content-agnostic in the sense that it can be used with 
any format or kind of data. A typical scenario is submitting a query for data extraction (e.g. SQL) to the broker (server) to be received by multiple clients which in turn process the query and return status/progress information and resulting extracted data. Other use cases include distribution of metadata/terminology and processing of material transfer requests.


As all communication is based on RESTful HTTP endpoints, the `broker-client` is optional and can be replaced with simple HTTP calls.


Example for a typical scenario:
```
Analyst          Broker-Server        Broker-Client-1        Broker-Client-2      ....

submit query --->   publish query
                        |
                        |    <-----     ask for queries
                        |                    |
                        |    ----->     receive new query
                        |                    |
                        |    <-----     report status
                        |                    | (internal workflow 
                        |                    | and query execution)
                        |    <-----     report results
                        |
                (collect responses)
                        |
get status update <---  |
                        |
                        |
                        |    <-----------------------------     ask for queries
                        |                                            |
                        |    ----------------------------->     receive new query
                        |                                            |
                        |    <-----------------------------     report status
                        |                                            | (internal workflow 
                        |                                            | and query execution)
                        |    <-----------------------------     report results
                        |
retrieve results <---   |

```



Getting started
===============
The easiest way to use the software is to download and run the [pre-build binary distribution](../../releases) of  `broker-admin-dist`.

Running the broker
------------------
To run the central broker component, first unpack the binary distribution `broker-admin-dist-1.x.zip`. For running the application, you need a Java 8 runtime environment or newer. We recommend to use the latest OpenJDK version (currently OpenJDK 15). Open a command shell in the extracted folder and run the script `run_broker.sh` for Linux/MingW/GitBash or `run_broker.bat` for Windows respectively. To change startup options (e.g port), edit the startup script.


Building from source code
-------------------------
To build the project from its source code, you need a Java runtime environment (e.g. [OpenJDK 15](https://jdk.java.net/15/), minimum is Java 8) and the build-tool [Apache Maven](https://maven.apache.org/download.cgi). To build the project, download/clone the repository source code and run `mvn clean install` via command shell in the main directory. After the build process is completed, you can find the broker binary distribution in the subfolder `broker-admin-dist/target`. To run the broker, see section *Getting startet* above.



Examples for using the broker
=============================

Below, you will find examples for typical use cases. For accessing the broker from Java/JRE-Applications, you can use the broker-client dependency (https://mvnrepository.com/artifact/org.aktin/broker-client) which communicates with the broker via HTTP. To demonstrate the simplicity of the RESTful API, the command line tool `curl` is used in the following examples for direct HTTP communication.

Submitting a request to the broker
------------------------------
This example performs authentication at the broker and creates a request containing query syntax which is to be distributed to all nodes.
```bash
# Admin authentication and store token in shell variable
TOKEN=`curl -s -H "Content-Type: application/xml" -X POST \
    -d '<credentials><username>admin</username><password>CHANGEME</password></credentials>' \
    http://localhost:8080/auth/login`

# Create a file containing the query syntax
echo "SELECT * FROM fhir_observation" > query1.sql

# submit the query
curl -i -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/sql" -X POST \
     -d @query1.sql http://localhost:8080/broker/request

# the response will contain a Location header for the newly created request. 
# We will use this location below to publish the request
curl -si -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8080/broker/request/1/publish

```
Documentation on the RESTful API of the request administration endpoint can be found in [RequestAdminEndpoint.java](broker-server/src/main/java/org/aktin/broker/RequestAdminEndpoint.java)


Retrieving a request at the client side
---------------------------------------
In this example, the client will authenticate via API-key and receives the published request.
```bash
# list available requests
curl -is -H "Authorization: Bearer xxxApiKey123" http://localhost:8080/broker/my/request

# retrieve first request
curl -is -H "Authorization: Bearer xxxApiKey123" http://localhost:8080/broker/my/request/1

# update status to retrieved
curl -is -H "Authorization: Bearer xxxApiKey123" -X POST \
     http://localhost:8080/broker/my/request/1/status/retrieved
```

Supplying results to a request from the client side
---------------------------------------------------
This example demonstrates, how the client can supply arbitrary data as a result to a request.
```bash
# Create a file containing the query results
echo -e "a;b\n1;2\n3;4\n" > result1.csv

# submit the file contents
curl -i -H "Authorization: Bearer xxxApiKey123" -H "Content-Type: text/csv" -X PUT \
     -d @result1.csv http://localhost:8080/aggregator/my/request/1/result

# update status to completed
curl -is -H "Authorization: Bearer xxxApiKey123" -X POST \
     http://localhost:8080/broker/my/request/1/status/completed

```



Updating the client status for a request
----------------------------------------
A different client may also reject a query. (Note the different API-key to indicate a different client)
```
curl -is -H "Authorization: Bearer xxxApiKey567" -X POST \
     http://localhost:8080/broker/my/request/1/status/rejected
```
Possible request states are documented in [broker-api/RequestStatus](broker-api/src/main/java/org/aktin/broker/xml/RequestStatus.java)



Download the collection of results for a query
----------------------------------------------
Once one or more clients have responded to a request e.g. by supplying result data,
the submitter can retrieve the results either via individual REST calls or more conveniently
via ZIP bundle:

```bash
# assuming authentication was already done (see first example)
# retrieve a download ID for the bundle containing all results and status updates
BUNDLE_ID=`curl -s -H "Authorization: Bearer $TOKEN"  http://localhost:8080/broker/export/request-bundle/1`

# download the results bundle
curl -s -H "Authorization: Bearer $TOKEN" --output results.zip \
     http://localhost:8080/broker/download/$BUNDLE_ID 

```


Using the web frontend
----------------------
The broker includes a minimal web frontent, which can be used to view the status of connected nodes,
manage requests and responses and download results. This frontent serves only as a built-in minimal 
user interface - for a better user experience, external customized frontents should be used.

To access the frontend, go to http://localhost:8080/admin/html/index.html once the broker is running.
For login, use the username *admin*. The admin password is specified in the startup script and defaults to *CHANGEME*.



Using the broker-client library
-------------------------------
To use broker functionality in java client applications, you can use the broker-client dependency 
(https://mvnrepository.com/artifact/org.aktin/broker-client). For a code example on how to use the client, see
the following implementations:
- https://github.com/li2b2/li2b2-shrine/tree/master/node/i2b2/i2b2-node
- https://github.com/li2b2/li2b2-shrine/tree/master/node/dktk/dktk-node
