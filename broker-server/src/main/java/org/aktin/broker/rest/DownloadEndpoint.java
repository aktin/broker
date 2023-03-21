package org.aktin.broker.rest;

import java.io.IOException;
import java.sql.Date;
import java.util.UUID;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.aktin.broker.download.Download;
import org.aktin.broker.download.DownloadManager;

/**
 * Provide temporary download links which expire after a short period of time or number of downloads.
 * 
 *
 * @author R.W.Majeed
 *
 */
@Path("/broker/download")
public class DownloadEndpoint {
	private static final Logger log = Logger.getLogger(DownloadEndpoint.class.getName());

	@Inject
	DownloadManager downloads;

	/**
	 * Retrieve a download. This method is not authenticated on purpose, as the download id
	 * can not be guessed and authentication was already required for creation of the download link.
	 * @param id download id
	 * @return download content stream
	 * @throws IOException IO error
	 */
	@GET
	@Path("{id}")
	public Response download(@PathParam("id") String id) throws IOException {
		System.err.println("Download requested for "+id);
		UUID uuid;
		Download download;
		try{
			uuid = UUID.fromString(id);
			download = downloads.get(uuid);
		}catch( IllegalArgumentException e ) {
			log.warning("Failed to parse UUID");
			download = null;
			uuid = null;
		}
		if( download == null ) {
			log.info("No download found with UUID "+uuid);
			throw new NotFoundException();
		}
		// add media type
		ResponseBuilder rb;
		rb = Response.ok(download.getInputStream(), download.getContentType());
		if( download.getLastModified() != null ) {
			rb.lastModified(Date.from(download.getLastModified()));
		}
		// add content length header if available
		Long contentLength = download.getContentLength();
		if( contentLength != null ) {
			rb.header(HttpHeaders.CONTENT_LENGTH, contentLength);
		}
		// add file name if available
		if( download.getName() != null ) {
			rb.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\""+download.getName()+"\"");
		}
		return rb.build();
	}
}
