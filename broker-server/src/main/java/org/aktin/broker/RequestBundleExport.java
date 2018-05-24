package org.aktin.broker;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.RequestStatusList;
import org.aktin.broker.xml.RequestTargetNodes;

public class RequestBundleExport {

	private BrokerBackend backend;
	private JAXBContext jaxb;
	
	public RequestBundleExport(BrokerBackend backend) throws JAXBException {
		this.backend = backend;
		jaxb = JAXBContext.newInstance(RequestStatusList.class, RequestTargetNodes.class);
	}

	public Path createBundle(int requestId) throws IOException {
		Path temp = Files.createTempFile("request-bundle", ".zip");
		createBundle(requestId, temp);
		return temp;
	}

	private void writeStatusList(int requestId, OutputStream out) throws SQLException, JAXBException {
		List<RequestStatusInfo> list = backend.listRequestNodeStatus(requestId);
		Objects.requireNonNull(list);
		jaxb.createMarshaller().marshal(new RequestStatusList(list), out);
	}

	private void createBundle(int requestId, Path path) throws IOException {
		
		try( ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(path)) ){
			// out.setComment
			out.setComment("Bundle for request "+requestId);
			ZipEntry entry;
			entry = new ZipEntry("status.xml");
			out.putNextEntry(entry);
			writeStatusList(requestId, out);
			out.closeEntry();
			// node restrictions
			int[] nodes = backend.getRequestTargets(requestId);
			if( nodes != null ) {
				entry = new ZipEntry("node-restriction.xml");
				jaxb.createMarshaller().marshal(new RequestTargetNodes(nodes), out);
				out.closeEntry();
			}
			// TODO add request content
			// TODO add node status/error messages
			// TODO also add request results/exported data?
		} catch (SQLException | JAXBException e) {
			throw new IOException(e);
		}
	}
}
