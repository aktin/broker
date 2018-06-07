package org.aktin.broker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.server.DateDataSource;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.RequestStatusList;
import org.aktin.broker.xml.RequestTargetNodes;
import org.aktin.broker.xml.ResultInfo;

public class RequestBundleExport {
	private static final Logger log = Logger.getLogger(RequestBundleExport.class.getName());

	private BrokerBackend backend;
	private JAXBContext jaxb;
	private AggregatorBackend aggregator;
	private Charset charset;
	
	public RequestBundleExport(BrokerBackend backend) throws JAXBException {
		this.backend = backend;
		jaxb = JAXBContext.newInstance(RequestInfo.class, RequestStatusList.class, RequestTargetNodes.class);
		this.charset = Charset.forName("UTF-8");
	}

	/**
	 * Configure the provided marshaller for improved readability. Adding spacing and line breaks.
	 * @param marshaller
	 */
	private void configureMarshaller(Marshaller marshaller) {
		try {
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setProperty(Marshaller.JAXB_ENCODING, charset.name());
		} catch (PropertyException e) {
			log.log(Level.WARNING,"Failed to configure JAXB marshaller for readability", e);
		}
	}
	/**
	 * Create and return a marshaller configured for ZIP exports.
	 * Uses {@link #configureMarshaller(Marshaller)} for configuration.
	 * @return marshaller
	 * @throws JAXBException JAXB error
	 */
	private Marshaller createMarshaller() throws JAXBException {
		Marshaller m = jaxb.createMarshaller();
		configureMarshaller(m);
		return m;
	}
	/**
	 * Set the aggregator. Used to include submitted result data in exports.
	 * @param aggregator aggergator
	 */
	public void setAggregator(AggregatorBackend aggregator) {
		this.aggregator = aggregator;
	}
//	public Path createBundle(int requestId) throws IOException {
//		Path temp = Files.createTempFile("request-bundle", ".zip");
//		createBundle(requestId, temp);
//		return temp;
//	}

	private void writeStatusList(int requestId, ZipOutputStream zip) throws SQLException, JAXBException, IOException {
		List<RequestStatusInfo> list = backend.listRequestNodeStatus(requestId);
		Objects.requireNonNull(list);
		ZipEntry entry = new ZipEntry("status.xml");
		zip.putNextEntry(entry);
		createMarshaller().marshal(new RequestStatusList(list), zip);
		zip.closeEntry();
		// add node status/error messages
		for( RequestStatusInfo status : list ) {
			if( status.type != null ) {
				// has status message
				try( Reader reader = backend.getRequestNodeStatusMessage(requestId, status.node) ){
					entry = new ZipEntry(status.node+"_info"+guessFileExtension(status.type));
					zip.putNextEntry(entry);
					transferReader(reader, zip, charset);
					zip.closeEntry();
				}
			}
		}

	}

	private void writeAggregatorResults(int requestId, ZipOutputStream zip) throws SQLException, IOException {
		Objects.requireNonNull(aggregator);
		int numTypes = aggregator.getDistinctResultTypes(requestId).length;
		for( ResultInfo info : aggregator.listResults(requestId) ) {
			String type = info.type;
			String ext = guessFileExtension(type);
			String name;
			if( numTypes == 1 ) {
				// only one type of result.
				// use simple name
				name = info.node+"_result"+ext;
			}else {
				// multiple result types
				// differentiate by type
				name = info.node+"_"+URLEncoder.encode(type, charset.name())+ext;
			}
			ZipEntry entry = new ZipEntry(name);
			DateDataSource dds = aggregator.getResult(requestId, Integer.parseInt(info.node));
			entry.setTime(dds.getLastModified().toEpochMilli());
			zip.putNextEntry(entry);
			try( InputStream in = dds.getInputStream() ){
				transferInput(in, zip);
			}
			zip.closeEntry();
		}
	}

	private String guessFileExtension(String mediaType) {
		// remove encoding and other arguments from media type
		String[] parts = mediaType.split(";");
		if( parts.length > 1 ) {
			mediaType = parts[0];
		}
		// guess extensions
		if( mediaType.endsWith("xml") ) {
			return ".xml";
		}
		else if( mediaType.endsWith("json") ) {
			return ".json";
		}
		else if( mediaType.startsWith("text") ) {
			return ".txt";
		}else if( mediaType.equals("application/zip") ) {
			return ".zip";
		}
		else return "";
	}

	private static void transferReader(Reader reader, OutputStream out, Charset charset) throws IOException {		
		CharsetEncoder encoder = charset.newEncoder();
		CharBuffer cb = CharBuffer.allocate(1024);
		ByteBuffer bb = ByteBuffer.allocate(2048);
		while( reader.read(cb) != -1 ) {
			cb.flip();
			encoder.encode(cb, bb, false);
			cb.compact();
			bb.flip();
			out.write(bb.array(), bb.arrayOffset()+bb.position(), bb.remaining());
			bb.position(bb.position()+bb.remaining());
			bb.compact();
		}
		cb.flip();
		// make sure no characters are remaining
		do {
			encoder.encode(cb, bb, true);	
			bb.flip();
			out.write(bb.array(), bb.arrayOffset()+bb.position(), bb.remaining());
			bb.position(bb.position()+bb.remaining());
			bb.compact();			
		}while( cb.hasRemaining() );
		encoder.flush(bb);
		bb.flip();
		out.write(bb.array(), bb.arrayOffset()+bb.position(), bb.remaining());
	}
	private static void transferInput(InputStream in, OutputStream out) throws IOException {
		int n;
		byte[] buffer = new byte[1024];
		while((n = in.read(buffer)) > -1) {
			out.write(buffer, 0, n);
		}
	}
	private void writeRequestDefinition(int requestId, String mediaType, OutputStream out) throws IOException, SQLException {
		try( Reader reader = backend.getRequestDefinition(requestId, mediaType) ){
			transferReader(reader, out, charset);			
		}
	}
	private void writeRequestData(int requestId, ZipOutputStream zip) throws SQLException, IOException, JAXBException {
		RequestInfo info = backend.getRequestInfo(requestId);
		Marshaller m = createMarshaller();
		ZipEntry entry;
		// write request info
		entry = new ZipEntry("request.xml");
		zip.putNextEntry(entry);
		m.marshal(info, zip);
		zip.closeEntry();
	
		// write request definitions
		if( info.types.length == 1 ) {
			// only single definition
			String type = info.types[0];
			String ext = guessFileExtension(type);
			entry = new ZipEntry("definition"+ext);
			zip.putNextEntry(entry);
			writeRequestDefinition(requestId, type, zip);
			zip.closeEntry();
		}else {
			// multiple definitions
			for( int i=0; i<info.types.length; i++ ) {
				String type = info.types[i];
				String ext = guessFileExtension(type);
				entry = new ZipEntry("definition_"+i+ext);
				zip.putNextEntry(entry);
				writeRequestDefinition(requestId, type, zip);
				zip.closeEntry();
			}
		}

	}

	public void createBundle(int requestId, OutputStream dest) throws IOException {
		try( ZipOutputStream out = new ZipOutputStream(dest) ){
		// out.setComment
			out.setComment("Bundle for request "+requestId);
			writeStatusList(requestId, out);
			// node restrictions
			int[] nodes = backend.getRequestTargets(requestId);
			if( nodes != null ) {
				ZipEntry entry = new ZipEntry("node-restriction.xml");
				out.putNextEntry(entry);
				createMarshaller().marshal(new RequestTargetNodes(nodes), out);
				out.closeEntry();
			}
			// add request content
			writeRequestData(requestId, out);
			
			// also add request results/exported data
			if( this.aggregator != null ) {
				writeAggregatorResults(requestId, out);
			}
		} catch (SQLException | JAXBException e) {
			throw new IOException(e);
		}		
	}
	public void createBundle(int requestId, Path path) throws IOException {
		
		try( OutputStream out = Files.newOutputStream(path) ){
			createBundle(requestId, out);
		}
	}
}
