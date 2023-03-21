package org.aktin.broker.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.util.RequestTypeManager;
import org.aktin.broker.websocket.RequestAdminWebsocket;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatus;

/**
 * Endpoint accessed by nodes
 * to work with their (my) data.
 *
 * @author R.W.Majeed
 *
 */
@Authenticated
@Path("/broker/my/")
public class MyBrokerEndpoint extends AbstractRequestEndpoint{
	private static final Logger log = Logger.getLogger(MyBrokerEndpoint.class.getName());
	@Inject
	private BrokerBackend db;
	@Inject
	private RequestTypeManager typeManager;

	/**
	 * Upload node resources to the broker
	 * @param resourceId resource id
	 * @param headers HTTP headers. Used to retrieve media type
	 * @param sec security context
	 * @param content resource content to store
	 */
	@PUT
	@Path("node/{resource}")
	public void setNodesResource(@PathParam("resource") String resourceId, @Context HttpHeaders headers, @Context SecurityContext sec, InputStream content) {
		Principal user = (Principal)sec.getUserPrincipal();
		int nodeId = user.getNodeId();
		log.info("Resource uploaded by node "+nodeId+": "+resourceId);
		try {
			db.updateNodeResource(user.getNodeId(), resourceId, headers.getMediaType(), content);
			RequestAdminWebsocket.broadcastNodeResourceChange(nodeId, resourceId);

		} catch (IOException | SQLException e) {
			log.log(Level.SEVERE, "Unable to write resource for node "+user.getNodeId()+": "+resourceId, e);
			throw new InternalServerErrorException(e);
		}
	}
	
	@GET
	@Path("node")
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

	// list requests for the calling node
	@GET
	@Path("request")
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
	
	@OPTIONS
	@Path("request/{id}")
	public RequestInfo getNodesRequestInfo(@PathParam("id") Integer requestId, @Context SecurityContext sec, @Context HttpHeaders headers) throws SQLException, IOException{
		RequestInfo info = db.getRequestInfo(requestId);
		if( info == null ){
			throw new NotFoundException("No request with id "+requestId);
		}
		return info;
	}
	
	@GET
	@Path("request/{id}")
	// response type depends on the data
	public Response getNodesRequest(@PathParam("id") Integer requestId, @Context SecurityContext sec, @Context HttpHeaders headers) throws SQLException, IOException{
//		Principal user = (Principal)sec.getUserPrincipal();
		List<MediaType> accept = headers.getAcceptableMediaTypes();
		// TODO for */* and only single request definition, return that definition
		Response resp = getRequest(requestId, accept);
		//  don't set the status automatically. the client might fail during storage. let the client set the #retrieved status
//		if( resp.getStatus() == 200 ){
//			// set retrieved timestamp
//			db.setRequestNodeStatus(requestId, user.getNodeId(), RequestStatus.retrieved, Instant.now());
//		}
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
	@POST
	@Path("request/{id}/status/{status}")
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
		RequestAdminWebsocket.broadcastRequestNodeStatus(requestId, user.getNodeId(), status.name());
		return Response.noContent().build();
	}
	
	@DELETE
	@Path("request/{id}")
	public void deleteNodesRequest(@PathParam("id") String requestId, @Context SecurityContext sec){
		Principal user = (Principal)sec.getUserPrincipal();
		boolean delete_ok = false;
		try {
			delete_ok = db.markRequestDeletedForNode(user.getNodeId(), Integer.parseInt(requestId));
		} catch (NumberFormatException e) {
			log.log(Level.WARNING, "Unable to parse request id="+requestId, e);
			delete_ok = false; // same as not found
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to delete requestId="+requestId+" for nodeId="+user.getNodeId(), e);
			throw new InternalServerErrorException();
		}
		if( false == delete_ok ){
			throw new NotFoundException();
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
