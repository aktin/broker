package org.aktin.broker;

import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.aktin.broker.auth.AuthCache;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.server.DateDataSource;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.NodeList;

@Path("/broker/node/")
public class NodeInfoEndpoint {
	private static final Logger log = Logger.getLogger(NodeInfoEndpoint.class.getName());
	@Inject
	private BrokerBackend db;
	@Inject
	private AuthCache auth; // for cached last contact timestamp

	@Context
	private Request request;
	
	/**
	 * Retrieve a list of registered nodes with the
	 * broker.
	 * @return JSON list of nodes
	 */
	@GET
	@Produces(MediaType.APPLICATION_XML)
	@RequireAdmin
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
	@Path("{id}")
	@Produces(MediaType.APPLICATION_XML)
	@RequireAdmin
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
	 * Retrieve a named resource uploaded previously by the specified node.
	 * <p>
	 * Last modified and eTag headers will be set.
	 * The eTag is calculated {@code url-safe-base64(sha-256(data))}.
	 * </p>
	 * @param nodeId node id
	 * @param resourceId resource id
	 * @return status {@code 200} with node info or status {@code 404} if not found. 
	 * @throws SQLException sql error
	 */
	@GET
	@Path("{node}/{resource}")
	@RequireAdmin
	public Response getNodeResource(@PathParam("node") int nodeId, @PathParam("resource") String resourceId) throws SQLException{		
		DateDataSource ds = db.getNodeResource(nodeId, resourceId);
		if( ds == null ){
			throw new NotFoundException();
		}
		Date lastModified = Date.from(ds.getLastModified());
		ResponseBuilder resp = request.evaluatePreconditions(lastModified);
		if( resp == null ){
			// timestamps different, resource modified
			resp = Response.ok(ds, ds.getContentType());
			// set etag
		}else{
			log.info("Resource not changed "+nodeId+"/"+resourceId);
		}
		if( ds instanceof DigestPathDataSource ){
			resp.tag(Base64.getUrlEncoder().encodeToString(((DigestPathDataSource)ds).sha256));
			resp.header("Content-MD5", Base64.getUrlEncoder().encodeToString(((DigestPathDataSource)ds).md5));
		}
		// set last modified
		resp.lastModified(lastModified);
		// add cache control header
		CacheControl cc = new CacheControl();
		cc.setMustRevalidate(true);
		cc.setMaxAge(60*5); // may cache for 5 minutes
		resp.cacheControl(cc);

		return resp.build();
	}
	
}
