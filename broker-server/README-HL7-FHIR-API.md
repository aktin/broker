
HL7 FHIR-API
============

Specification for a HL7 FHIR API for basic request broker and federated query execution operations.

A request is wrapped in a `Task` resource. E.g.
```xml
<Task>
	<status value="draft"/><!-- see supported status values below -->
	<intent value="proposal"/><!-- fixed -->
	<!-- recipient restriction is optional. if present, only the
	listed organizations are able to access the request -->
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
				<system value="http://aktin.org/ns/fhir/taskinputtypes">
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
	<!-- in the admin FHIR-endpoint, there will be one output per query response. 
	typically one reply per participating organization. -->
	<output>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir/organization">
				<code value="12"><!-- organization id/code submitting the result -->
			</coding>
			<coding>
				<system value="http://hl7.org/fhir/task-status">
				<code value="failed"><!-- individual status of query at specified organization --> 
			</coding>
		</type>
		<!-- in case of failed status, attachent can contain an error message or might be empty -->
		<valueAttachment/>
	</output>
	<output>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir/organization">
				<code value="13">
			</coding>
			<coding>
				<system value="http://hl7.org/fhir/task-status">
				<code value="completed">
			</coding>
		</type>
		<valueAttachment>
			<!-- query result content type and content -->
			<contentType value="application/csv"/>
			<url value="/aggregator/result/123"/>
		</valueAttachment>
	</output>
</Task>

```
The on creation, the task status must be set to `draft`. In this state,
the task is not available to nodes. When the status is set by the admin to `requested`, 
it will be published and can be retrieved by clients.
When a task is closed by the admin, its status is set to 'cancelled'.


Node/Client FHIR endpoint
=========================

For node/client access to the broker, the base URI is typically `/my/fhir` or `/broker/my/fhir`.
Listed below are the possible operations for a broker client.

# List all/pending requests
```
GET /my/fhir/Task?_summary=true
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
GET /my/fhir/Task/123
```
# Update node request status
```
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
PATCH /my/fhir/Task/123
<diff>
 <replace sel="Task/status/@value">failed</replace>
</diff>
```
Allowed status updates are `received`, `accepted` (queued), `rejected`, `on-hold` (interaction), `in-progress`, `failed`, `completed`.



# Uppload result data for a specific query
```
PATCH /my/fhir/Task/123
<diff>
 <add sel="Task">
  <output>
		<type>
			<coding>
				<system value="http://aktin.org/ns/fhir/taskoutputtypes">
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
```
{
  "lastUpdated":"2016-06-01T10:36:23.232-05:00",
  "location":"Task/123",
  "operationType":"update",
  "resourceId":"123",
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
