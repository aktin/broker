package org.aktin.broker;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.NodeList;
import org.aktin.broker.xml.NodeStatus;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.RequestStatusList;

/**
 * Broker service.
 * 
 * TODO allow JSON media types (via JAXB mapping e.g. with MOXY)
 * TODO add RequestTypeManager
 * @author R.W.Majeed
 *
 */
@Path(BrokerEndpoint.SERVICE_URL)
public class BrokerEndpoint {
	private static final Logger log = Logger.getLogger(BrokerEndpoint.class.getName());
	public static final String SERVICE_URL = "/broker/";

	@Inject
	private BrokerBackend db;

	@Inject
	RequestTypeManager typeManager;

	/**
	 * Retrieve status information about the broker.
	 * @return JSON status
	 */
	@GET
	@Path("status")
	@Produces(MediaType.APPLICATION_XML)
	public BrokerStatus status(){
		return BrokerStatus.create();
	}
	
	
	/**
	 * Retrieve a list of registered nodes with the
	 * broker.
	 * @return JSON list of nodes
	 */
	@GET
	@Path("all")
	@Produces(MediaType.APPLICATION_XML)
	public Response allNodes(){
		try {
			return Response.ok(new NodeList(db.getAllNodes())).build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "unable to retrieve node list", e);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Report the local node status to the broker.
	 * @param status JSON status
	 */
	@Authenticated
	@POST
	@Path("my/status")
	@Consumes(MediaType.APPLICATION_XML)
	public void reportNodesStatus(NodeStatus status, @Context SecurityContext sec){
		log.info("Node status retrieved");
	}
	@Authenticated
	@GET
	@Path("my/request")
	public Response listNodesRequests(@Context SecurityContext sec){
		Principal user = (Principal)sec.getUserPrincipal();
		try {
			return Response.ok(new RequestList(db.listRequestsForNode(user.getNodeId()))).build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to read requests for nodeId="+user.getNodeId(), e);
			return Response.serverError().build();
		}
	}
	@Authenticated
	@GET
	@Path("my/request/{id}")
	public Response getNodesRequest(@PathParam("id") Integer requestId, @Context SecurityContext sec, @Context HttpHeaders headers) throws SQLException, IOException{
		Principal user = (Principal)sec.getUserPrincipal();
		Response resp = getRequest(requestId, headers);
		if( resp.getStatus() == 200 ){
			// set retrieved timestamp
			db.setRequestNodeStatus(requestId, user.getNodeId(), RequestStatus.retrieved, Instant.now());
		}
		return resp;
	}

	/**
	 * Status report by a node for it's request.
	 * 
	 * @param requestId request to which the status belongs
	 * @param status reported status. A status of {@link RequestStatus#deleted} is equal to deleting
	 * a request via {@link #deleteNodesRequest(String, SecurityContext)}.
	 * 
	 * @param sec security context
	 * @param headers
	 * 		request headers. Use the {@code Date} header to specify a timestamp for the status.
	 * 		If {@code Date} is not specified, the current time is used.
	 * @param content
	 *	status message. Use the {@code Content-Type} header to specify a media type.
	 *	only character content is allowed (including XML/JSON).
	 * @return
	 * @throws SQLException
	 * @throws IOException
	 */
	@Authenticated
	@POST
	@Path("my/request/{id}/status/{status}")
	public Response putNodesRequestError(@PathParam("id") Integer requestId, 
			@PathParam("status") RequestStatus status, 
			@Context SecurityContext sec, @Context HttpHeaders headers, Reader content){

		Principal user = (Principal)sec.getUserPrincipal();
		Date date = headers.getDate();
		if( date == null ){
			// no date specified, use current time
			date = new Date();
		}
		try {
			db.setRequestNodeStatus(requestId, user.getNodeId(), status, date.toInstant());
			if( headers.getMediaType() != null ){
				// TODO set status text
			}
			content.close();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to update status for requestId="+requestId+" for nodeId="+user.getNodeId(), e);
			return Response.serverError().build();
		} catch (IOException e) {
			log.log(Level.WARNING, "Unable to close content reader", e);
		}
		return Response.noContent().build();
	}
	
	@Authenticated
	@DELETE
	@Path("my/request/{id}")
	public Response deleteNodesRequest(@PathParam("id") String requestId, @Context SecurityContext sec){
		Principal user = (Principal)sec.getUserPrincipal();
		boolean delete_ok = false;
		try {
			delete_ok = db.markRequestDeletedForNode(user.getNodeId(), Integer.parseInt(requestId));
		} catch (NumberFormatException e) {
			log.log(Level.WARNING, "Unable to parse request id="+requestId, e);
			delete_ok = false; // same as not found
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to delete requestId="+requestId+" for nodeId="+user.getNodeId(), e);
			return Response.serverError().build();
		}
		if( delete_ok ){
			return Response.noContent().build();
		}else{
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	private static MediaType removeCharsetInfo(MediaType type){
		// TODO other media type parameters are not preserved (e.g. ;version=1.2, do we need these?
		return new MediaType(type.getType(), type.getSubtype());
	}
	//TODO @RequireAdminCert
	@POST
	@Path("request")
	public Response createRequest(Reader content, @Context HttpHeaders headers) throws URISyntaxException{
		MediaType type = headers.getMediaType();
		try {
			// remove charset information, since we already have the string representation
			type = removeCharsetInfo(type);
			int id = db.createRequest(type.toString(), content);
			return Response.created(new URI(SERVICE_URL+"request/"+Integer.toString(id))).build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to create request", e);
			return Response.serverError().build();
		}
	}
	// TODO @RequireAdminCert
	@PUT
	@Path("request/{id}")
	public Response createRequest(@PathParam("id") String requestId, Reader content, @Context HttpHeaders headers) throws URISyntaxException{
		MediaType type = headers.getMediaType();
		try {
			// remove charset information, since we already have the string representation
			type = removeCharsetInfo(type);
			// TODO check if request exists
			db.addRequestDefinition(Integer.parseInt(requestId), type.toString(), content);
			return Response.ok().build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to create request definition", e);
			return Response.serverError().build();
		} catch( NumberFormatException e ){
			log.log(Level.SEVERE, "Unable to parse request id: "+requestId, e);
			return Response.status(Status.BAD_REQUEST).build();
		}
	}
	
	@GET
	@Path("request")
	public Response listAllRequests() {
		try {
			return Response.ok(new RequestList(db.listAllRequests())).build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to read requests", e);
			return Response.serverError().build();			
		}
	}
	// TODO @RequireAdminCert
	@DELETE
	@Path("request/{id}")
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
		} catch( NumberFormatException e ){
			return Response.status(404).build();
		}
	}
	@GET
	@Path("request/{id}")
	public Response getRequest(@PathParam("id") Integer requestId, @Context HttpHeaders headers) throws SQLException, IOException{
			List<MediaType> accept = headers.getAcceptableMediaTypes();
		MediaType[] available = typeManager.createMediaTypes(db.getRequestTypes(requestId));
		// find acceptable request definition
		RequestConverter rc = typeManager.buildConverterChain(accept, Arrays.asList(available));

		if( rc == null ){
			// no acceptable response type available
			return Response.notAcceptable(Variant.mediaTypes(available).build()).build();			
		}else{
			Reader def = db.getRequestDefinition(requestId, rc.getConsumedType());
			// transform
			def = rc.transform(def);
			return Response.ok(def, MediaType.valueOf(rc.getProducedType())).build();
		}
	}
	@OPTIONS
	@Path("request/{id}")
	public Response getRequestInfo(@PathParam("id") String requestId, @Context HttpHeaders headers) throws SQLException, IOException{
		// TODO return RequestInfo
		List<RequestInfo> list = db.listAllRequests();
		RequestInfo info = null;
		for( RequestInfo ri : list ){
			if( ri.getId().equals(requestId) ){
				info = ri;
				break;
			}
		}
		ResponseBuilder response;
		if( info == null ){
			response = Response.status(Status.NOT_FOUND);
		}else{
			response = Response.ok(info);
		}
		return response.allow("GET","PUT","DELETE","OPTIONS").build();
	}
	/**
	 * List status information for the specified request for each node
	 * @param requestId request
	 * @return status information list
	 * @throws SQLException
	 * @throws IOException
	 */
	@GET
	@Path("request/{id}/status")
	public Response getRequestInfo(@PathParam("id") Integer requestId) throws SQLException, IOException{
		// TODO return RequestInfo
		List<RequestStatusInfo> list = db.listRequestNodeStatus(requestId);
		
		if( list == null ){
			return Response.status(Status.NOT_FOUND).build();
		}else{
			return Response.ok(new RequestStatusList(list)).build();
		}
	}

	// TODO @RequireAdminCert
	@POST
	@Path("request/{id}/publish")
	public Response publishRequest(@PathParam("id") String requestId, String timestamp){
		// TODO find query
		Instant.parse(timestamp);
		// TODO update published timestamp
		// TODO broadcast to connected websocket clients
		return Response.noContent().build();
	}
}
