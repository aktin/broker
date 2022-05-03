
HL7 FHIR-API for federated request executions
============

Specification for a HL7 FHIR API for basic request broker and federated query execution operations.

This specification is currently a draft and not fully implemented.

API description for request administration from requestor (central) point of view.
-------------


Retrieve all registered organizations
```
GET /Organization

```
Will return a search result collection with all registered organizations


Retrieve a specific organization
```
GET /Organization/123

```
Will return a single organization resource




Retrieve all main tasks
```
GET /Task?part-of:missing=true
or
GET /Task?code=feasibility-search

```




Retrieve execution status for each node which retrieved the task 1.

```
GET /Task?part-of=1&_summary=true

```
Will return a search collection of all subtasks belonging to the specified main task.



Create a request
```
POST /Task
```

A request always represented as a `Task` resource. E.g.
```xml
<Task>
	<status value="draft"/><!-- see supported status values below -->
	<intent value="proposal"/><!-- constant -->
	<code>
		<coding>
			<system value="http://aktin.org/ns/fhir"/>
			<code value="feasibility-search">
		</coding>
	</code>
	<!-- recipient restriction is optional. if present, only the
	listed organizations are able to access the request.
	if missing, applies to any organization. -->
	<restriction>
		<recipient>
			<reference value="/Organization/1"/>
		</recipient>
		<recipient>
			<reference value="/Organization/2"/>
		</recipient>
	</restriction>
	<!-- the request input/query can be specified in multiple different definition media types.
	Upon creation,the input can be omitted and added later via `add request definition` operation
	before the request task is published. -->
	<input>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir">
				<code value="request-definition">
			</coding>
		</type>
		<valueAttachment>
			<contentType value="application/sql"/>
			<!-- one of the following -->
			<data value="U0VMRUNUIDE="/> <!-- inline data is used when submitting a request definition syntax -->
			<url value="/my/request/123"/> <!-- url data used when a task is retrieved. the content type must be specified during retrieval -->
		</valueAttachment>
	</input>
	<input>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir/taskinputtypes">
				<code value="request-definition">
			</coding>
		</type>
		<valueAttachment>
			<contentType value="application/cql"/>
			<url value="/my/request/123"/>
		</valueAttachment>
	</input>
</Task>

```
The on creation, the task status must be set to `draft`. In this state,
the task is not available to nodes. When the status is set by the admin to `requested`, 
it will be published and can be retrieved by clients.
When a task is closed by the admin, its status is set to 'cancelled' (or completed?).


For each submitted response, a result is added to the task. Each result corresponds to a subtask automatically created and assigned to the main task.
```
	<!-- in the admin FHIR-endpoint, there will be one output per query response. 
	typically one reply per participating organization. -->
	<output>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir">
				<code value="node-subtask">
			</coding>
		</type>
		<valueReference>
			<reference value="Task/1-1"/>
			<type value="Task"/>
		</valueReference>
	</output>
	<output>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir">
				<code value="node-subtask">
			</coding>
		</type>
		<valueReference>
			<reference value="Task/1-2"/>
			<type value="Task"/>
		</valueReference>
	</output>
	<!-- a summary type may be added with aggregated results. alternatively a virtual subtask containing the summary result? -->
	<output>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir">
				<code value="result-summary">
			</coding>
		</type>
		<valueAttachment>
			<!-- query result content type and content -->
			<contentType value="application/csv"/>
			<url value="/aggregator/result/123"/>
		</valueAttachment>
	</output>
```


Publish a request
```
PATCH /Task/123
```
Request body
```xml
<diff>
 <replace sel="Task/status/@value">requested</replace>
</diff>
```

Cancel a request. Alternatively the request could be deleted.
```
PATCH /Task/123
```
Request body
```xml
<diff>
 <replace sel="Task/status/@value">cancelled</replace>
</diff>
```

Delete a request.
```
DELETE /Task/123
```





Node/Client FHIR endpoint
=========================

For node/client access to the broker, the base URI is typically `/my/fhir` or `/broker/my/fhir`.
Listed below are the possible operations for a broker client.

# List all/pending requests
Request
```
GET /my/fhir/Task
or
GET /my/fhir/Task?authored-on=ge2013-01-14T10:00
or
GET /my/fhir/Task?code=feasibility-execution&status=requested
```
Response
```xml
<Bundle>
...
  <entry>
    <resource>
      <Task>
        <id value="123"/>
        ...
      </Task>
    </resource>
  </entry>
  ...
</Bundle>
```

# Retrieve a particular request
Request
```
GET /my/fhir/Task/1-123
```
Response is a task resource similar to the example above, but without `output` elements.

```xml
<Task>
	<status value="requested"/><!-- will always be 'requested' when the task is available to the node initially -->
	<intent value="proposal"/><!-- fixed -->
	<!-- one or more input entries will be present -->
	<input>
	...
	</input>
	...
</Task>
```

# Report updated status for a particular request
Request
```
PATCH /my/fhir/Task/1-123
```
Request body
```xml
<diff>
 <replace sel="Task/status/@value">failed</replace>
 <!-- add error message as output attachment -->
 <add sel="Task">
   <output>
     <type>
       <coding>
         <system value="http://aktin.org/ns/fhir"/>
         <code value="error-message"/>
       </coding>
     </type>
     <valueString value="Concept not found: xyz"/>
   </output>
 </add>
</diff>
```
Allowed status updates are `received`, `accepted` (queued), `rejected`, `on-hold` (interaction), `in-progress`, `failed`, `completed`.



# Uppload result data for a specific query
Request
```
PATCH /my/fhir/Task/1-123
```
Request body
```xml
<diff>
 <add sel="Task">
  <output>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir">
				<code value="query-result">
			</coding>
		</type>
		<valueAttachment>
			<contentType value="text/csv"/>
			<data value="ZGlhZ25vc2lzO2NvdW50ClY5Ny4zM1hEOzM="/>
		</valueAttachment>
  </output>
 </add>
</diff>
```

# Delete node request
```
DELETE /my/fhir/Task/123
```

# Websocket notifications
Advertised by the `websocket` extension in the capability statement.
Will usually be the URL `/my/fhir/notification`.
Once a client is subscribed, it will retrieve events in the form
```json
{
  "lastUpdated":"2016-06-01T10:36:23.232-05:00",
  "location":"Task/123",
  "operationType":"update",
  "resourceId":"123"
}
```
This is implemented analoguous to the IBM FHIR server.


Request administration FHIR API endpoint
========================================
For request administration, as separate FHIR base URI is used. Typically `/broker/fhir`


In addition to operation similar to the ones above, the admin can perform the following actions:
```
GET /fhir/Organization (list all nodes known to the broker)
GET /fhir/Organization/123 (retrieve specific node information)
POST /fhir/Task (create request)
PATCH /fhir/Task/123 (add request definition, add node restriction, change status to published or closed)
GET /fhir/Task (retrieve request)
DELETE /fhir/Task/123 (delete request)
ws:/fhir/notification (websocket for update notifications)
```
