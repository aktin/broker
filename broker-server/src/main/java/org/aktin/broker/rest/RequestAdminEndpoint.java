package org.aktin.broker.rest;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.notify.MyBrokerWebsocket;
import org.aktin.broker.util.RequestTypeManager;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.RequestStatusList;
import org.aktin.broker.xml.RequestTargetNodes;

/**
 * RESTful HTTP endpoint to manage broker requests.
 * 
 * Request management is not done by client nodes. For client node usage,
 * see {@link MyBrokerEndpoint}.
 * 
 * @author R.W.Majeed
 *
 */
@Path("/broker/request")
public class RequestAdminEndpoint extends AbstractRequestEndpoint{
	private static final Logger log = Logger.getLogger(RequestAdminEndpoint.class.getName());
	@Inject
	private BrokerBackend db;
	@Inject
	private RequestTypeManager typeManager;



	/***
	 * Create a new request at the broker and return the request location in the HTTP-response 
	 * header {@code Location}.
	 * 
	 * This method can be used either to create an empty request if no {@code Content-type} header 
	 * is given (e.g. to add multiple content representations later)  or it can be used to create 
	 * a request already containing a request definition. 
	 * In the latter case the {@code Content-type} header must be present and the HTTP payload is 
	 * used for the representation. 
	 * 
	 * @param content HTTP payload to use as first content representation
	 * @param headers HTTP request headers containing Content-type
	 * @param info URI info for the response location
	 * @return request object with status 201 and location header.
	 * @throws URISyntaxException URI syntax processing error
	 */
	@POST
	@RequireAdmin
	public Response createRequest(Reader content, @Context HttpHeaders headers, @Context UriInfo info) throws URISyntaxException{
		MediaType type = headers.getMediaType();
		try {
			int id;
			
			if( type != null ){
				// remove charset information, since we already have the string representation
				type = removeCharsetInfo(type);
				id = db.createRequest(type.toString(), content);
			}else{
				// no content type passed
				// TODO verify that also no content is given (data without content type)
				id = db.createRequest();
			}
			String ref = "/broker/request/"+Integer.toString(id);
			// may return the wrong scheme (eg http instead of https) behind reverse proxies
			//return Response.created(new URI(ref)).build();
			// allow override via system property
			UriBuilder ub = info.getBaseUriBuilder().path(ref);
			String forceScheme = System.getProperty("force.uri.scheme");
			if( forceScheme != null ){
				log.info("Forcing response location URI scheme "+forceScheme);
				ub.scheme(forceScheme);
			}
			return Response.created(ub.build()).build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to create request", e);
			return Response.serverError().build();
		}
	}

	/**
	 * Add (additional) request definitions to an existing request. This method is similar 
	 * to {@link #createRequest(Reader, HttpHeaders, UriInfo)} but expects the request 
	 * to be already existing.
	 * 
	 * @param requestId request id
	 * @param content request definition
	 * @param headers Content-type header
	 * @return HTTP 200 on success
	 * @throws URISyntaxException URI syntax error
	 */
	@PUT
	@Path("{id}")
	@RequireAdmin
	public Response createRequest(@PathParam("id") String requestId, Reader content, @Context HttpHeaders headers) throws URISyntaxException{
		MediaType type = headers.getMediaType();
		try {
			// remove charset information, since we already have the string representation
			type = removeCharsetInfo(type);
			// create or replace if already exists
			db.setRequestDefinition(Integer.parseInt(requestId), type.toString(), content);
			return Response.ok().build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to create request definition", e);
			return Response.serverError().build();
		} catch( NumberFormatException e ){
			log.log(Level.SEVERE, "Unable to parse request id: "+requestId, e);
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	/**
	 * List all request available at this broker
	 * 
	 * @return HTTP 200 with XML representation of all requests
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@RequireAdmin
	public Response listAllRequests() {
		try {
			return Response.ok(new RequestList(db.listAllRequests())).build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to read requests", e);
			return Response.serverError().build();			
		}
	}

	/**
	 * Delete a single existing request
	 * 
	 * @param id request id to delete
	 * @return HTTP 200 on success
	 */
	@DELETE
	@Path("{id}")
	@RequireAdmin
	public Response deleteRequest(@PathParam("id") String id){
		int i;
		try{
			i = Integer.parseInt(id);
		}catch( NumberFormatException e ){
			// cannot delete non-numeric request id
			log.warning("Unable to delete non-numeric request id: "+id);
			return Response.status(Status.BAD_REQUEST).build();
		}
		try {
			db.deleteRequest(i);
			return Response.noContent().build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to delete request "+id, e);
			return Response.serverError().build();
		}
	}

	/**
	 * Get a single content representation for the given request which matches the provided Accept header.
	 * 
	 * @param requestId request id request id
	 * @param headers headers headers containing acceptable media types
	 * @return request definition matching the Accept header
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 * @throws NotFoundException request id does not exist
	 */
	@RequireAdmin
	@GET
	@Path("{id}")
	public Response getRequest(@PathParam("id") Integer requestId, @Context HttpHeaders headers) throws SQLException, IOException, NotFoundException{
			List<MediaType> accept = headers.getAcceptableMediaTypes();
			return getRequest(requestId, accept);
	}

	/**
	 * Retrieve available HTTP methods for the given request ID
	 * 
	 * @param requestId request id
	 * @return response with [@code Allow} header or 404 if request was not found 
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 */
	@OPTIONS
	@Path("{id}")
	@RequireAdmin
	//@Produces(MediaType.APPLICATION_XML) will cause errors in this case. therefore the media type is set below
	public Response getRequestInfo(@PathParam("id") int requestId) throws SQLException, IOException{
		// TODO return RequestInfo
		RequestInfo info = db.getRequestInfo(requestId);
		ResponseBuilder response;
		if( info == null ){
			response = Response.status(Status.NOT_FOUND);
		}else{
			response = Response.ok(info, MediaType.APPLICATION_XML_TYPE);
		}
		return response.allow("GET","PUT","DELETE","OPTIONS").build();
	}
	/**
	 * List status information for the specified request for each node
	 * @param requestId request
	 * @return status information list
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 */
	@GET
	@Path("{id}/status")
	@RequireAdmin
	@Produces(MediaType.APPLICATION_XML)
	public RequestStatusList getRequestInfo(@PathParam("id") Integer requestId) throws SQLException, IOException{
		// TODO return RequestInfo
		List<RequestStatusInfo> list = db.listRequestNodeStatus(requestId);
		if( list == null ){
			throw new NotFoundException();
		}else{
			return new RequestStatusList(list);
		}
	}
	/**
	 * Get the targeted nodes for this request. A resource is only returned, if 
	 * the request is limited to / targeted at specifig nodes.
	 * @param requestId request
	 * @return target node list
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 * @throws NotFoundException if the request is not targeted at specific nodes
	 */
	@GET
	@Path("{id}/nodes")
	@Produces(MediaType.APPLICATION_XML)
	@RequireAdmin
	public RequestTargetNodes getRequestTargetNodes(@PathParam("id") Integer requestId) throws SQLException, IOException, NotFoundException{
		int[] nodes = db.getRequestTargets(requestId);
		if( nodes == null ){
			throw new NotFoundException();
		}
		return new RequestTargetNodes(nodes);
	}
	

	/**
	 * Delete a restriction to certain target nodes. When the delete is successful, the
	 * request can be retrieved by all nodes.
	 * @param requestId request id
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 * @throws NotFoundException request not found or request not targeted at specific nodes
	 */
	@DELETE
	@Path("{id}/nodes")
	@RequireAdmin
	public void clearRequestTargetNodes(@PathParam("id") Integer requestId) throws SQLException, IOException, NotFoundException{
		// TODO check for valid requestId and throw NotFoundException otherwise
		int[] nodes = db.getRequestTargets(requestId);
		if( nodes == null ){
			throw new NotFoundException();
		}
		db.clearRequestTargets(requestId);
	}

	/**
	 * Apply a publication restriction to the request, making it available only to the given nodes.
	 * Only the specified nodes will be able to retrieve the request.
	 *
	 * To clear the restriction, submit an empty restriction list. Without the restriction,
	 * any node can download the request - also including nodes added in the future.
	 * 
	 * @param requestId request id
	 * @param nodes selected list of nodes to which the request should be available
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 * @throws NotFoundException request not found
	 */
	@PUT
	@Path("{id}/nodes")
	@Consumes(MediaType.APPLICATION_XML)
	@RequireAdmin
	public void setRequestTargetNodes(@PathParam("id") Integer requestId, RequestTargetNodes nodes) throws SQLException, IOException, NotFoundException{
		if( nodes == null || nodes.getNodes() == null || nodes.getNodes().length == 0 ){
			String message = "node targeting requires at least one node";
			log.warning(message);
			throw new BadRequestException(message);
		}
		// TODO replacing / changing nodes deletes all status information for the previous node. Find a way/restrictions to handle this case
		// XXX 
		db.setRequestTargets(requestId, nodes.getNodes());
	}

	/**
	 * Retrieve request node status message (e.g. error messages) assigned to a request by a node
	 * 
	 * @param requestId request id
	 * @param nodeId node id
	 * @return error payload
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 */
	@GET
	@Path("{id}/status/{nodeId}")
	@RequireAdmin
	public Response getRequestNodeStatusMessage(@PathParam("id") Integer requestId, @PathParam("nodeId") Integer nodeId) throws SQLException, IOException{
		// TODO set header: timestamp, custom header with status code
		Reader r = db.getRequestNodeStatusMessage(requestId, nodeId);
		if( r == null ){
			throw new NotFoundException();
		}
		// TODO retrieve and return exact media type
		return Response.ok(r, MediaType.TEXT_PLAIN).build();
	}



	/**
	 * Publish the given request. After being published, the request can be retrieved
	 * by client nodes.
	 *
	 * @param requestId request id
	 * @throws SQLException SQL error
	 */
	@POST
	@Path("{id}/publish")
	@RequireAdmin
	public void publishRequest(@PathParam("id") Integer requestId) throws SQLException{
		// find query
		RequestInfo info = db.getRequestInfo(requestId);
		if( info == null ){
			// 404 if not found
			throw new NotFoundException();
		}else if( info.published != null ){
			; // already published, nothing to do
		}else{
			// TODO use timestamp from headers for future publishing

			// update published timestamp
			db.setRequestPublished(requestId, Instant.now());

			// broadcast to connected websocket clients
			MyBrokerWebsocket.broadcastRequestPublished(requestId);
		}
	}

	/**
	 * Mark a request as closed.
	 * The client nodes will stop processing requests in closed state.
	 *
	 * @param requestId request id
	 * @throws SQLException SQL error
	 */
	@POST
	@Path("{id}/close")
	@RequireAdmin
	public void closeRequest(@PathParam("id") Integer requestId) throws SQLException{
		// find query
		RequestInfo info = db.getRequestInfo(requestId);
		if( info == null ){
			// 404 if not found
			throw new NotFoundException();
		}else if( info.closed != null ){
			; // already closed, nothing to do
			
		}else{
			// update published timestamp
			db.setRequestClosed(requestId, Instant.now());

			// broadcast to connected websocket clients
			MyBrokerWebsocket.broadcastRequestClosed(requestId);
		}
	}
	@Override
	protected RequestTypeManager getTypeManager() {
		return typeManager;
	}
	@Override
	protected BrokerBackend getBroker() {
		return db;
	}

}
