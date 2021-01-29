package org.aktin.broker.rest;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

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
@RequireAdmin
@Path("/broker/export")
public class ExportEndpoint {
		@Inject
		private BrokerBackend broker;
		@Inject
		private AggregatorBackend aggregator;

		@Inject
		private DownloadManager downloads;

		@GET
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
