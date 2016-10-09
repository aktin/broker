== broker-query ==
Kommunikationsschnittstelle auf Brokerseite. Verschickt verteilte Queries an alle Standorte.

Each broker has a public/private keypair with signature of certification authority.
The data ware houses connect to the broker and 

The Broker provides different /Query Set/s to query clients and data warehouses.
A /query set/ consists of the following parts:

1. The observation RDF ontology consists of concepts, which
can be queried. This ontology needs a mapping at the data warehouse
side to participate in queries. The mapping can be done automatically,
if standard terminology is used on both sides (e.g. SNOMED-CT)

2. /Strukturdaten/ ontology, describing data outside of patient
context. E.g. Number of patients per year, number of beds, geographic location of site,
coordinates of "Einzugsgebiet", state, country, name of site, contact person.

3. Data pool url, where the queried patient data should be sent. This can be 
a relative URL at the broker or point to a different server. If a different server,
the broker will communicate the query IDs to the pool.

-- Data Warehouse interaction --
A fresh data warehouse can be configured to talk/associate to several brokers. For each
associated broker, it needs a client certificate (as the broker identifies the clients
by their certificate).

The data warehouse will then request query sets from the broker and try automatic 
mapping of the respective ontologies.

=== Cascaded brokers (extension) ===
A broker can also function as a virtual data warehouse and register with
another broker. For each associated broker, the respective ontologies will be
mapped to it's own /query sets/. Constraints can be set, to which downstream 
data warehouses the upstream queries are forwarded.


=== Erste Version ===
Nichts Programmieren. Benutzung über Aufrufe mit curl zum Verschicken von XML.
