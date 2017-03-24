package org.aktin.broker;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
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
import javax.ws.rs.core.Response.Status;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.notify.BrokerWebsocket;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatus;

/**
 * Endpoint accessed by nodes
 * to work with their (my) data.
 *
 * @author R.W.Majeed
 *
 */
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
	@Authenticated
	@PUT
	@Path("node/{resource}")
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
	@Authenticated
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
	@Authenticated
	@GET
	@Path("request/{id}")
	// response type depends on the data
	public Response getNodesRequest(@PathParam("id") Integer requestId, @Context SecurityContext sec, @Context HttpHeaders headers) throws SQLException, IOException{
		Principal user = (Principal)sec.getUserPrincipal();
		List<MediaType> accept = headers.getAcceptableMediaTypes();

		Response resp = getRequest(requestId, accept);
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
		BrokerWebsocket.broadcastRequestNodeStatus(requestId, user.getNodeId(), status.name());
		return Response.noContent().build();
	}
	
	@Authenticated
	@DELETE
	@Path("request/{id}")
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

	@Override
	protected RequestTypeManager getTypeManager() {
		return typeManager;
	}

	@Override
	protected BrokerBackend getBroker() {
		return db;
	}

}
