package org.aktin.broker.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXB;

import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.util.Util;
import org.w3c.dom.Document;

@Deprecated
public class BrokerClientImpl extends AbstractBrokerClient implements BrokerClient{	
	public BrokerClientImpl(URI brokerEndpoint) {
		super(brokerEndpoint);
		// TODO retrieve aggregator URL from broker info
		setAggregatorEndpoint(brokerEndpoint.resolve("/aggregator/"));
	}
	
//	@FunctionalInterface
	public static interface OutputWriter{
		void write(OutputStream dest) throws IOException;
		public static class ForString implements OutputWriter{
			private String str;
			private String enc;
			public ForString(String str, String encoding){
				this.str = str;
				this.enc = encoding;
			}
			@Override
			public void write(OutputStream dest) throws IOException {
				writeContent(str, dest, enc);
			}
			
		}
	}
	

	@Override
	protected URI getQueryBaseURI() {
		return resolveBrokerURI("my/request/");
	}

	@Override
	public BrokerStatus getBrokerStatus() throws IOException{
		HttpURLConnection c = openConnection("GET", "status");
		try( InputStream response = c.getInputStream() ){
			return JAXB.unmarshal(response, BrokerStatus.class);
		}
	}
	/**
	 * Post the supplied software versions under a resource {@code versions}.
	 * Due to limitations in the way Java persists {@link Properties}, {@code null} values
	 * are not allowed.
	 * @param softwareVersions map with software names as keys and versions as values.
	 * @throws IOException IO error during transmission
	 * @throws NullPointerException for {@code null} values in the map
	 */
	@Override
	public void postSoftwareVersions(Map<String,String> softwareVersions) throws IOException, NullPointerException{
		Properties map = new Properties();
		map.putAll(softwareVersions);
		putMyResourceProperties("versions", map);
	}
	/**
	 * Post status for the client node. Additional software modules can be specified.
	 * 
	 * @param startupEpochMillis startup time in epoch milliseconds
	 * @param softwareVersions software module versions
	 * @param payload Status content / payload. Can be any JAXB compatible object ({@code @XMLRootElement} or {@link Element})
	 * @throws IOException IO error
	 */
//	@Deprecated
//	public void postMyStatus(long startupEpochMillis, Map<String,String> softwareVersions) throws IOException{
//		postSoftwareVersions(softwareVersions);
//	}
	/**
	 * Get list of requests
	 * @return request list
	 * @throws IOException error
	 */
	@Override
	public List<RequestInfo> listMyRequests() throws IOException{
		HttpURLConnection c = openConnection("GET", "my/request");
		RequestList list = null;
		try( InputStream response = c.getInputStream() ){
			list = JAXB.unmarshal(response, RequestList.class);
		}
		return postprocessRequestList(list);
	}
	/**
	 * Get info for a single request
	 * @param id request id
	 * @return request list
	 * @throws IOException error
	 */
	@Override
	public RequestInfo getRequestInfo(int id) throws IOException{
		HttpURLConnection c = openConnection("OPTIONS", getQueryURI(id));
		RequestInfo info = null;
		try( InputStream response = c.getInputStream() ){
			info = JAXB.unmarshal(response, RequestInfo.class);
		}
		return info;
	}
	@Override
	public void deleteMyRequest(int id) throws IOException{
		delete(getQueryURI(id));
	}
	@Override
	public Reader getMyRequestDefinitionReader(int id, String mediaType) throws IOException{
		HttpURLConnection c = openConnection("GET", getQueryURI(id));
		return contentReader(c, mediaType);
	}
	@Override
	public Node getMyNode() throws IOException{
		HttpURLConnection c = openConnection("GET", "my/node");
		try( InputStream response = c.getInputStream() ){
			return JAXB.unmarshal(response, Node.class);
		}
	}
	@Override
	public Document getMyRequestDefinitionXml(int id, String mediaType) throws IOException{
		Reader reader = getMyRequestDefinitionReader(id, mediaType);
		if( reader == null ){
			// not found
			return null;
		}
		try{
			return Util.parseDocument(reader);
		}finally{
			reader.close();
		}
	}
	@Override
	public String[] getMyRequestDefinitionLines(int id, String mediaType) throws IOException{
		ArrayList<String> lines = new ArrayList<>();
		Reader def = getMyRequestDefinitionReader(id, mediaType);
		if( def == null ){
			// not found
			return null;
		}
		try( BufferedReader reader = new BufferedReader(def) ){
			for( String line=reader.readLine(); line!=null; line = reader.readLine() ){
				lines.add(line);
			}
		}
		return lines.toArray(new String[lines.size()]);
	}
	@Override
	public String getMyRequestDefinitionString(int id, String mediaType) throws IOException{
		StringBuilder builder = new StringBuilder();
		Reader def = getMyRequestDefinitionReader(id, mediaType);
		if( def == null ){
			// not found
			return null;
		}
		try( BufferedReader reader = new BufferedReader(def) ){
			char[] buf = new char[2048];
			while( true ){
				int len = reader.read(buf, 0, buf.length);
				if( len == -1 ){
					break;
				}
				builder.append(buf, 0, len);
			}
		}
		return builder.toString();
	}
//	@SuppressWarnings("unchecked")
//	public <T> T getMyRequestDefinition(String id, String mediaType, Class<T> type) throws IOException{
//		if( type == String.class ){
//			return (T)getMyRequestDefinitionString(id, mediaType);
//		}else{
//			throw new IllegalArgumentException("Unsupported type "+type);
//		}
//	}
	@Override
	public void putMyResource(String name, String contentType, final InputStream content) throws IOException{
		putResource(resolveBrokerURI("my/node/").resolve(name), contentType, new OutputWriter(){
			@Override
			public void write(OutputStream dest) throws IOException {
				writeContent(dest, content);
			}
		});
	}
	@Override
	public void putMyResourceXml(String name, Object jaxbObject) throws IOException{
		putResource(resolveBrokerURI("my/node/").resolve(name), "application/xml", new OutputWriter(){
			@Override
			public void write(OutputStream dest) throws IOException {
				JAXB.marshal(jaxbObject, dest);
			}
		});
	}
	@Override
	public void putMyResourceProperties(String name, Properties properties) throws IOException{
		putResource(resolveBrokerURI("my/node/").resolve(name), "application/xml; charset=utf-8", new OutputWriter(){
			@Override
			public void write(OutputStream dest) throws IOException {
				properties.storeToXML(dest, name, "UTF-8");
			}
		});
	}
	@Override
	public Properties getMyResourceProperties(String name) throws IOException{
		Properties props;
		NodeResource res = getMyResource(name);
		// verify content type
		if( !res.getContentType().startsWith("application/xml") ){
			throw new IOException("Unexpected content type: "+res.getContentType());
		}
		try( InputStream response = res.getInputStream() ){
			props = new Properties();
			props.loadFromXML(response);
		}
		return props;
	}
	@Override
	public NodeResource getMyResource(String name) throws IOException{
		HttpURLConnection c = openConnection("GET", resolveBrokerURI("my/node/").resolve(URLEncoder.encode(name, "UTF-8")));
		return wrapResource(c, name);
	}
	@Override
	public void putMyResource(String name, String contentType, String content) throws IOException{
		putResource(resolveBrokerURI("my/node/").resolve(name), contentType, new OutputWriter(){
			@Override
			public void write(OutputStream dest) throws IOException {
				writeContent(content, dest, "UTF-8");
			}
		});
	}
	@Override
	public void deleteMyResource(String name) throws IOException{
		delete(resolveBrokerURI("my/node/").resolve(name));
	}
	
	// aggregator functions
	private void putResource(URI uri, String contentType, OutputWriter writer) throws IOException{
		HttpURLConnection c = openConnection("PUT", uri);
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", contentType);
		try( OutputStream out = c.getOutputStream() ){
			writer.write(out);
		}
		c.getInputStream().close();
	}
	private void putRequestResult(int requestId, String contentType, OutputWriter writer) throws IOException{
		URI putResult = resolveAggregatorURI("my/request/"+requestId+"/result");
		putResource(putResult, contentType, writer);
	}
	@Override
	public void putRequestResult(int requestId, String contentType, final InputStream content) throws IOException{
		putRequestResult(requestId, contentType, new OutputWriter(){
			@Override
			public void write(OutputStream dest) throws IOException {
				writeContent(dest, content);
			}
		});
	}
	@Override
	public void putRequestResult(int requestId, String contentType, String content) throws IOException{
		putRequestResult(requestId, contentType+";charset=UTF-8", new OutputWriter.ForString(content, "UTF-8"));
	}
	@Override
	public void postRequestStatus(int requestId, RequestStatus status) throws IOException{
		HttpURLConnection c = openConnection("POST", getQueryBaseURI().resolve(requestId+"/status/"+status.name()));
		c.getInputStream().close();
	}
	@Override
	public void postRequestFailed(int requestId, String message, Throwable throwable) throws IOException{
		HttpURLConnection c = openConnection("POST", getQueryBaseURI().resolve(requestId+"/status/"+RequestStatus.failed.name()));
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
		// write the error content
		try( OutputStream post = c.getOutputStream();
				PrintWriter writer = new PrintWriter(new OutputStreamWriter(post, StandardCharsets.UTF_8))){
			if( message != null ){
				writer.write(message);
			}
			if( throwable != null ){
				if( message != null ){
					// add newline to separate the message from the stacktrace
					writer.println();
				}
				throwable.printStackTrace(writer);				
			}
		}
		// there should be no content. reading/closing the input stream 
		// will make sure the status code is in the 2xx group.
		c.getInputStream().close();
	}
//	public void postRequestStatus(int requestId, RequestStatus status, Instant date) throws IOException{
//		HttpURLConnection c = openConnection("POST", getQueryBaseURI().resolve(requestId+"/status/"+status.name()));
//		c.setRequestProperty("Date", Util.formatHttpDate(date));
//		c.getInputStream().close();	
//	}
	@Override
	public void postRequestStatus(int requestId, RequestStatus status, Instant date, String description) throws IOException{
		HttpURLConnection c = openConnection("POST", getQueryBaseURI().resolve(requestId+"/status/"+status.name()));
		if( date != null ){
			c.setRequestProperty("Date", Util.formatHttpDate(date));
		}
		if( description != null ){
			c.setDoOutput(true);
			c.setRequestProperty("Content-Type", "text/plain;charset=UTF-8");
			try( OutputStream post = c.getOutputStream();
					PrintWriter writer = new PrintWriter(new OutputStreamWriter(post, StandardCharsets.UTF_8))){
				writer.write(description);
			}
		}
		c.getInputStream().close();
	}

	// for i2b2, request application/xml+i2b2
	// for dkdk, request application/xml+samply
	public static void main(String[] args) throws Exception{
//		KeyStore ks = KeyStore.getInstance("PKCS12");
//		FileInputStream fis = new FileInputStream("/path/to/file.p12");
//		ks.load(fis, "password".toCharArray());
//		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//		kmf.init(ks, "password".toCharArray());
//		SSLContext sc = SSLContext.getInstance("TLS");
//		sc.init(kmf.getKeyManagers(), null, null);		
		SSLContext sc = SSLContext.getInstance("TLSv1.2");
		// Init the SSLContext with a TrustManager[] and SecureRandom()
		sc.init(null, null, new java.security.SecureRandom());
		URL url = new URL("https://localhost:12345/idm/lala/");
		HttpsURLConnection c = (HttpsURLConnection)url.openConnection();
		c.setSSLSocketFactory(sc.getSocketFactory());
		
	}
}
