package org.aktin.broker;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
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
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.aktin.broker.auth.AuthCache;
import org.aktin.broker.auth.Principal;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.notify.BrokerWebsocket;
import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.NodeList;
import org.aktin.broker.xml.NodeStatus;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.RequestStatusList;
import org.aktin.broker.xml.RequestTargetNodes;

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
	private AuthCache auth; // for cached last contact timestamp

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
	@Path("node")
	@Produces(MediaType.APPLICATION_XML)
	public Response allNodes(){
		try {
			List<Node> nodes = db.getAllNodes();
			auth.fillCachedAccessTimestamps(nodes);
			return Response.ok(new NodeList(nodes)).build();
			// look up cached last contact in AuthCache
		} catch (SQLException e) {
			log.log(Level.SEVERE, "unable to retrieve node list", e);
			return Response.serverError().build();
		}
	}
	/**
	 * Retrieve information about a single node.
	 * @param nodeId node id
	 * @return status {@code 200} with node info or status {@code 404} if not found. 
	 */
	@GET
	@Path("node/{id}")
	@Produces(MediaType.APPLICATION_XML)
	public Node getNodeInfo(@PathParam("id") int nodeId){
		Node node;
		try {
			node = db.getNode(nodeId);
			if( node == null ){
				throw new NotFoundException();
			}
			// look up cached last contact in AuthCache
			auth.fillCachedAccessTimestamps(Collections.singletonList(node));
		} catch (SQLException e) {
			log.log(Level.SEVERE, "unable to retrieve node list", e);
			throw new InternalServerErrorException(e);
		}
		return node;
	}
	/**
	 * Retrieve a named resource uploaded previously by the specified node
	 * @param nodeId node id
	 * @return status {@code 200} with node info or status {@code 404} if not found. 
	 * @throws SQLException sql error
	 */
	@GET
	@Path("node/{node}/{resource}")
	public DataSource getNodeResource(@PathParam("node") int nodeId, @PathParam("resource") String resourceId) throws SQLException{
		return db.getNodeResource(nodeId, resourceId);
	}

//	@GET
//	@Deprecated // TODO getNodeResource
//	@Path("node/{id}/status")
//	@Produces(MediaType.APPLICATION_XML)
//	public String getNodeStatus(@PathParam("id") int nodeId){
//		String content;
//		try {
//			content = db.getNodeStatusContent(nodeId);
//		} catch (SQLException e) {
//			log.log(Level.SEVERE, "unable to retrieve node status", e);
//			throw new InternalServerErrorException(e);
//		}
//		if( content == null ){
//			throw new NotFoundException();
//		}
//		return content;
//	}
	
	/**
	 * Report the local node status to the broker.
	 * @param status JSON status
	 * @param sec security context
	 * @throws SQLException 
	 */
	@Authenticated
	@POST
	@Deprecated // TODO replace with my/node/versions
	@Path("my/status")
	@Consumes(MediaType.APPLICATION_XML)
	public void reportNodesStatus(NodeStatus status, @Context SecurityContext sec) throws SQLException{
		Principal user = (Principal)sec.getUserPrincipal();
		log.info("Node status retrieved");
		// TODO calculate network delay via local and remote timestamps
		// store software module versions
		db.updateNodeStatus(user.getNodeId(), status);
	}
	/**
	 * Upload node resources to the broker
	 * @param status JSON status
	 * @param sec security context
	 * @throws SQLException 
	 */
	@Authenticated
	@PUT
	@Path("my/node/{resource}")
	public void setNodesResource(@PathParam("resource") String resourceId, @Context HttpHeaders headers, @Context SecurityContext sec, InputStream content) {
		Principal user = (Principal)sec.getUserPrincipal();
		log.info("Resource uploaded by node "+user.getNodeId()+": "+resourceId);
		try {
			db.updateNodeResource(user.getNodeId(), resourceId, headers.getMediaType(), content);
		} catch (IOException | SQLException e) {
			log.log(Level.SEVERE, "Unable to write resource for node "+user.getNodeId()+": "+resourceId, e);
			throw new InternalServerErrorException(e);
		}
	}
	
	@Authenticated
	@GET
	@Path("my/node")
	@Produces(MediaType.APPLICATION_XML)
	// TODO add unit test
	public Node getOwnNodeInfo(@Context SecurityContext sec){
		Principal user = (Principal)sec.getUserPrincipal();
		try {
			return db.getNode(user.getNodeId());
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to read node info for nodeId="+user.getNodeId(), e);
			throw new InternalServerErrorException(e);
		}
	}
	@Authenticated
	@GET
	@Path("my/request")
	@Produces(MediaType.APPLICATION_XML)
	public RequestList listNodesRequests(@Context SecurityContext sec){
		Principal user = (Principal)sec.getUserPrincipal();
		try {
			return new RequestList(db.listRequestsForNode(user.getNodeId()));
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to read requests for nodeId="+user.getNodeId(), e);
			throw new InternalServerErrorException(e);
		}
	}
	@Authenticated
	@GET
	@Path("my/request/{id}")
	// response type depends on the data
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
	 * @param status reported status
	 * a request via {@link #deleteNodesRequest(String, SecurityContext)}.
	 * 
	 * @param sec security context
	 * @param headers
	 * 		request headers. Use the {@code Date} header to specify a timestamp for the status.
	 * 		If {@code Date} is not specified, the current time is used.
	 * @param content
	 *	status message. Use the {@code Content-Type} header to specify a media type.
	 *	only character content is allowed (including XML/JSON).
	 * @return no content on success. Server error otherwise.
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
				// clear charset information, since we already have the string representation
				MediaType messageType = removeCharsetInfo(headers.getMediaType());
				db.setRequestNodeStatusMessage(requestId, user.getNodeId(), messageType.toString(), content);
			}
			content.close();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to update status for requestId="+requestId+" for nodeId="+user.getNodeId(), e);
			return Response.serverError().build();
		} catch (IOException e) {
			log.log(Level.WARNING, "Unable to close content reader", e);
		}
		BrokerWebsocket.broadcastRequestNodeStatus(requestId, user.getNodeId(), status.name());
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
//	@Authenticated
//	@RequireAdmin
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
//	@Authenticated
//	@RequireAdmin
	@PUT
	@Path("request/{id}")
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
	
	@GET
	@Path("request")
	@Produces(MediaType.APPLICATION_XML)
	public Response listAllRequests() {
		try {
			return Response.ok(new RequestList(db.listAllRequests())).build();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to read requests", e);
			return Response.serverError().build();			
		}
	}
//	@Authenticated
//	@RequireAdmin
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
		}
	}
	@GET
	@Path("request/{id}")
	public Response getRequest(@PathParam("id") Integer requestId, @Context HttpHeaders headers) throws SQLException, IOException, NotFoundException{
			List<MediaType> accept = headers.getAcceptableMediaTypes();
		MediaType[] available = typeManager.createMediaTypes(db.getRequestTypes(requestId));
		if( available.length == 0 ){
			throw new NotFoundException();
		}
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
	//@Produces(MediaType.APPLICATION_XML) will cause errors in this case. therefore the media type is set below
	public Response getRequestInfo(@PathParam("id") int requestId, @Context HttpHeaders headers) throws SQLException, IOException{
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
	@Path("request/{id}/status")
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
	@Path("request/{id}/nodes")
	@Produces(MediaType.APPLICATION_XML)
	public RequestTargetNodes getRequestTargetNodes(@PathParam("id") Integer requestId) throws SQLException, IOException, NotFoundException{
		int[] nodes = db.getRequestTargets(requestId);
		if( nodes == null ){
			throw new NotFoundException();
		}
		return new RequestTargetNodes(nodes);
	}
	@DELETE
	@Path("request/{id}/nodes")
	public void clearRequestTargetNodes(@PathParam("id") Integer requestId) throws SQLException, IOException, NotFoundException{
		db.clearRequestTargets(requestId);
	}
	@PUT
	@Path("request/{id}/nodes")
	@Consumes(MediaType.APPLICATION_XML)
	public void setRequestTargetNodes(@PathParam("id") Integer requestId, RequestTargetNodes nodes) throws SQLException, IOException, NotFoundException{
		db.setRequestTargets(requestId, nodes.getNodes());
	}

	// TODO add method to retrieve request node status message (e.g. error messages)
	@GET
	@Path("request/{id}/status/{nodeId}")
	public Response getRequestNodeStatusMessage(@PathParam("id") Integer requestId, @PathParam("nodeId") Integer nodeId) throws SQLException, IOException{
		// TODO set header: timestamp, custom header with status code
		Reader r = db.getRequestNodeStatusMessage(requestId, nodeId);
		if( r == null ){
			throw new NotFoundException();
		}
		// TODO retrieve and return exact media type
		return Response.ok(r, MediaType.TEXT_PLAIN).build();
	}
	
//	@Authenticated
//	@RequireAdmin
	@POST
	@Path("request/{id}/publish")
	public void publishRequest(@PathParam("id") Integer requestId) throws SQLException{
		// find query
		RequestInfo info = db.getRequestInfo(requestId);
		if( info == null ){
			// 404 if not found
			throw new NotFoundException();
		}else if( info.published != null ){
			// already published, nothing to do
			
		}else{
			// TODO use timestamp from headers for future publishing

			// update published timestamp
			db.setRequestPublished(requestId, Instant.now());

			// broadcast to connected websocket clients
			BrokerWebsocket.broadcastRequestPublished(requestId);
		}
	}
//	@Authenticated
//	@RequireAdmin
	@POST
	@Path("request/{id}/close")
	public void closeRequest(@PathParam("id") Integer requestId) throws SQLException{
		// find query
		RequestInfo info = db.getRequestInfo(requestId);
		if( info == null ){
			// 404 if not found
			throw new NotFoundException();
		}else if( info.closed != null ){
			// already closed, nothing to do
			
		}else{
			// update published timestamp
			db.setRequestClosed(requestId, Instant.now());

			// broadcast to connected websocket clients
			BrokerWebsocket.broadcastRequestClosed(requestId);
		}
	}

}
