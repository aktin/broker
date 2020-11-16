AKTIN Broker 
============
A content-agnostic data exchange middle-ware for federated data warehouses.

This software is not designed for standalone usage. Rather it is to be used within federated
data warehouse environments like e.g. research networks or multi-center clinical study registries 
in conjunction with local workflows and processing infrastructure.


The broker infrastructure contains two main components: The central component `broker-server` is used to publish information to be distributed. The local component `broker-client` is used by multiple parties to retrieve the published information and subsequently report status updates and response-data.
As all communication is based on RESTful HTTP endpoints, the `broker-client` is optional and can be replaced with simple HTTP calls.


Example for a typical scenario:
```
Analyst          Broker-Server        Broker-Client-1        Broker-Client-2      ....

ask query --->   publish query
                        |
                        |    <-----     ask for query
                        |                    |
                        |    ----->     receive query
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
                        |    <-----------------------------     ask for query
                        |                                            |
                        |    ----------------------------->     receive query
                        |                                            |
                        |    <-----------------------------     report status
                        |                                            | (internal workflow 
                        |                                            | and query execution)
                        |    <-----------------------------     report results
                        |
retrieve results <---   |

```