package org.aktin.broker.rest;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXBException;

import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.download.Download;
import org.aktin.broker.download.DownloadManager;
import org.aktin.broker.download.RequestBundleExport;

/**
 * Create export bundles for download
 * 
 * @author R.W.Majeed
 */
@Path("/broker/export")
public class ExportEndpoint {
		@Inject
		private BrokerBackend broker;
		@Inject
		private AggregatorBackend aggregator;

		@Inject
		private DownloadManager downloads;

		@Authenticated
		@RequireAdmin
		@POST
		@Path("request-bundle/{id}")
		@Produces(MediaType.TEXT_PLAIN)
		public String downloadBundle(@PathParam("id") int requestId) throws IOException, JAXBException {
			RequestBundleExport export = new RequestBundleExport(broker);
			export.setAggregator(aggregator);
			Download d = downloads.createTemporaryFile("application/zip", "export_"+Integer.toString(requestId)+".zip");
			export.createBundle(requestId, d.getOutputStream());
			return d.getId().toString();
		}

}
