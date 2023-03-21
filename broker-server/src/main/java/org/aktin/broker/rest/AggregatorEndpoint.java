package org.aktin.broker.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.download.Download;
import org.aktin.broker.download.DownloadManager;
import org.aktin.broker.download.RequestBundleExport;
import org.aktin.broker.server.DateDataSource;
import org.aktin.broker.util.FileStreamingResponse;
import org.aktin.broker.util.PathDataSource;
import org.aktin.broker.websocket.RequestAdminWebsocket;
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
	public void submitResult(@PathParam("id") String requestId, @HeaderParam("Content-type") MediaType type, @Context SecurityContext sec, InputStream content) throws URISyntaxException{
		Principal user = (Principal)sec.getUserPrincipal();
//		MediaType type = headers.getMediaType();
		if( type == null ) {
			throw new BadRequestException("required Content-type header missing");
		}
		int nodeId = user.getNodeId();
		log.info("Result received from node "+nodeId+": "+type.toString());
		int request = Integer.parseInt(requestId);
		// TODO catch numberformatexception and return error
		if( !isRequestWritable(request, user.getNodeId()) ){
			throw new ForbiddenException();
		}
		try {
			db.addOrReplaceResult(request, nodeId, type, content);
			RequestAdminWebsocket.broadcastNodeResult(request, nodeId, type.toString());
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to persist data", e);
			throw new InternalServerErrorException();
		}
	}
	
	@Authenticated
	@RequireAdmin
	@GET
	@Path("request/{id}/result")
	@Produces(MediaType.APPLICATION_XML)
	//@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	public ResultList listResultsForRequest(@PathParam("id") String requestId){
		int request = Integer.parseInt(requestId);
		List<ResultInfo> results;
		try {
			results = db.listResults(request);
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to list results from database", e);
			throw new InternalServerErrorException();
		}
		log.info("Found "+results.size()+" results");
		return new ResultList(results);
	}

	@Authenticated
	@RequireAdmin
	@DELETE
	@Path("request/{id}/result")
	public void deleteResultsForRequest(@PathParam("id") String requestId){
		int request = Integer.parseInt(requestId);
		try {
			db.deleteResults(request);
		} catch (SQLException | IOException e) {
			log.log(Level.SEVERE, "Failed to delete results from database", e);
			throw new InternalServerErrorException();
		}
	}


	private DateDataSource getResultForNode(int requestId, int nodeId) throws InternalServerErrorException, NotFoundException{
		DateDataSource data;
		try {
			data = db.getResult(requestId, nodeId);
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to retrieve data", e);
			throw new InternalServerErrorException(e);
		}
		if( data == null ){
			// no data for this request/node combination
			throw new NotFoundException();
		}else{
			return data;
		}
	}
	/**
	 * Create a download id to download the result uploaded for the request id and node id.
	 * @param requestId request id of the desired result
	 * @param nodeId node id of the desired result
	 * @return download id to be used with the download endpoint
	 */
	@Authenticated
	@RequireAdmin
	@POST
	@Produces(MediaType.TEXT_PLAIN)
	@Path("request/{id}/result/{nodeId}/download")
	public String createResultNodeDownload(@PathParam("id") int requestId, @PathParam("nodeId") int nodeId){
		DateDataSource data = getResultForNode(requestId, nodeId);
		String ext = RequestBundleExport.guessFileExtension(data.getContentType());
		if( ext == null ) {
			ext = "";
		}
		Download download = downloads.createDataSourceDownload(data, requestId+"_result_"+nodeId+ext);
		return download.getId().toString();
	}

	/**
	 * Create a download id to download the result uploaded for the request id and node id.
	 * @param requestId request id of the desired result
	 * @param nodeId node id of the desired result
	 * @return download id to be used with the download endpoint
	 */
	@Authenticated
	@RequireAdmin
	@GET
	@Path("request/{id}/result/{nodeId}")
	public Response getResultNodeDataStream(@PathParam("id") int requestId, @PathParam("nodeId") int nodeId){
		DateDataSource data = getResultForNode(requestId, nodeId);
		// this should be changed later to use the interface e.g. passing the path via interface
		if( !(data instanceof PathDataSource) ) {
			throw new InternalServerErrorException("Unexpected interface for result data source");
		}
		PathDataSource path = (PathDataSource)data;

		return Response.ok(new FileStreamingResponse(path.getPath()), data.getContentType())
			.lastModified(Date.from(data.getLastModified()))
			.header("Content-length", data.getContentLength())
			.build();
	}

}
