package org.aktin.broker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.xml.bind.JAXBException;

import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.db.BrokerBackend;

/**
 * Create export bundles for download
 * 
 * @author R.W.Majeed
 */
@Path("/broker/export")
public class ExportEndpoint {
		private static final Logger log = Logger.getLogger(ExportEndpoint.class.getName());
		@Inject
		private BrokerBackend broker;
		@Inject
		private AggregatorBackend aggregator;


		@GET
		@Path("request-bundle/{id}")
		@Produces("application/zip")
		public InputStream downloadBundle(@PathParam("id") int requestId) throws IOException, JAXBException {
			RequestBundleExport export = new RequestBundleExport(broker);
			export.setAggregator(aggregator);
			java.nio.file.Path path = export.createBundle(requestId);
			log.info("Export bundle created at "+path);
			return Files.newInputStream(path, StandardOpenOption.DELETE_ON_CLOSE);
		}

}
