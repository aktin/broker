package org.aktin.broker.rest;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.download.Download;
import org.aktin.broker.download.DownloadManager;
import org.aktin.broker.download.RequestBundleExport;
import org.aktin.broker.server.DateDataSource;
import org.aktin.broker.xml.ResultInfo;
import org.aktin.broker.xml.ResultList;

@Path(AggregatorEndpoint.SERVICE_URL)
public class AggregatorEndpoint {
	private static final Logger log = Logger.getLogger(AggregatorEndpoint.class.getName());
	public static final String SERVICE_URL = "/aggregator/";

	@Inject
	private AggregatorBackend db;
	@Inject
	private DownloadManager downloads;

	private boolean isRequestWritable(int requestId, int nodeId){
		// check if request is open for writing results (e.g. not closed)
		return db.isRequestWritable(requestId, nodeId);
	}

	// TODO allow multiple media types for a single result.
	@Authenticated
	@PUT
	@Path("my/request/{id}/result")
	public Response submitResult(@PathParam("id") String requestId, @Context HttpHeaders headers, @Context SecurityContext sec, InputStream content) throws URISyntaxException{
		Principal user = (Principal)sec.getUserPrincipal();
		MediaType type = headers.getMediaType();
		log.info("Result received from node "+user.getNodeId()+": "+type.toString());
		int request = Integer.parseInt(requestId);
		// TODO catch numberformatexception and return error
		if( !isRequestWritable(request, user.getNodeId()) ){
			return Response.status(Status.FORBIDDEN).build();
		}
		try {
			db.addOrReplaceResult(request, user.getNodeId(), type, content);
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to persist data", e);
			return Response.serverError().build();
		}
		return Response.noContent().build();
	}
	
//	@Authenticated
	@RequireAdmin
	@GET
	@Path("request/{id}/result")
	@Produces(MediaType.APPLICATION_XML)
	//@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public Response listResultsForRequest(@PathParam("id") String requestId){
		int request = Integer.parseInt(requestId);
		List<ResultInfo> results;
		try {
			results = db.listResults(request);
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to list results from database", e);
			return Response.serverError().build();
		}
		log.info("Found "+results.size()+" results");
		return Response.ok(new ResultList(results)).build();
	}

//	@Authenticated
	@RequireAdmin
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("request/{id}/result/{nodeId}")
	public String listResultsForRequest(@PathParam("id") String requestId, @PathParam("nodeId") String nodeId){
		int request = Integer.parseInt(requestId);
		int node = Integer.parseInt(nodeId);
		DateDataSource data;
		try {
			data = db.getResult(request, node);
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to retrieve data", e);
			throw new InternalServerErrorException(e);
		}
		if( data == null ){
			// no data for this request/node combination
			throw new NotFoundException();
		}else{
			String ext = RequestBundleExport.guessFileExtension(data.getContentType());
			if( ext == null ) {
				ext = "";
			}
			Download download = downloads.createDataSourceDownload(data, requestId+"_result_"+nodeId+ext);
			return download.getId().toString();
		}
	}

}
