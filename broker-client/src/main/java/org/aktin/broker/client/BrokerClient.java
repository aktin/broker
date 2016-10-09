package org.aktin.broker.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.xml.bind.JAXB;

import org.aktin.broker.xml.NodeStatus;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.SoftwareModule;
import org.aktin.broker.xml.util.Util;
import org.w3c.dom.Document;

public class BrokerClient extends AbstractBrokerClient{	
	public BrokerClient(URI brokerEndpoint) {
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
	public void postMyStatus(long startupEpochMillis, Iterable<SoftwareModule> software) throws IOException{
		HttpURLConnection c = openConnection("POST", "my/status");
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", "application/xml");
		NodeStatus status = new NodeStatus(new Date(startupEpochMillis), software);
		try( OutputStream post = c.getOutputStream() ){
			JAXB.marshal(status, post);
		}
		// this will throw an IOException if the status is not successful
		c.getInputStream().close();
	}
	public void postMyStatus(long startupEpochMillis, SoftwareModule ...modules) throws IOException{
		postMyStatus(startupEpochMillis, Arrays.asList(modules));
	}
	public List<RequestInfo> listMyRequests() throws IOException{
		HttpURLConnection c = openConnection("GET", "my/request");
		RequestList list = null;
		try( InputStream response = c.getInputStream() ){
			list = JAXB.unmarshal(response, RequestList.class);
		}
		return postprocessRequestList(list);
	}
	public void deleteMyRequest(String id) throws IOException{
		delete(getQueryURI(id));
	}
	public Reader getMyRequestDefinitionReader(String id, String mediaType) throws IOException{
		HttpURLConnection c = openConnection("GET", getQueryURI(id));
		return contentReader(c, mediaType);
	}
	public Document getMyRequestDefinitionXml(String id, String mediaType) throws IOException{
		try( Reader reader = getMyRequestDefinitionReader(id, mediaType) ){
			return Util.parseDocument(reader);
		}
	}
	public String[] getMyRequestDefinitionLines(String id, String mediaType) throws IOException{
		ArrayList<String> lines = new ArrayList<>();
		try( BufferedReader reader = new BufferedReader(getMyRequestDefinitionReader(id, mediaType)) ){
			for( String line=reader.readLine(); line!=null; line = reader.readLine() ){
				lines.add(line);
			}
		}
		return lines.toArray(new String[lines.size()]);
	}
	public String getMyRequestDefinitionString(String id, String mediaType) throws IOException{
		StringBuilder builder = new StringBuilder();
		try( BufferedReader reader = new BufferedReader(getMyRequestDefinitionReader(id, mediaType)) ){
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

	// aggregator functions
	public void putRequestResult(String requestId, String contentType, OutputWriter writer) throws IOException{
		URI putResult = resolveAggregatorURI("my/request/"+requestId+"/result");
		HttpURLConnection c = openConnection("PUT", putResult);
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", contentType);
		try( OutputStream out = c.getOutputStream() ){
			writer.write(out);
		}
		c.getInputStream().close();
	}
	public void putRequestResult(String requestId, String contentType, final InputStream content) throws IOException{
		putRequestResult(requestId, contentType, new OutputWriter(){
			@Override
			public void write(OutputStream dest) throws IOException {
				writeContent(dest, content);
			}
		});
	}
	public void putRequestResult(String requestId, String contentType, String content) throws IOException{
		putRequestResult(requestId, contentType+";charset=UTF-8", new OutputWriter.ForString(content, "UTF-8"));
	}
	public void postRequestStatus(String requestId, RequestStatus status) throws IOException{
		HttpURLConnection c = openConnection("POST", getQueryBaseURI().resolve(requestId+"/status/"+status.name()));
		c.getInputStream().close();
	}
	public void postRequestStatus(String requestId, RequestStatus status, Instant date) throws IOException{
		HttpURLConnection c = openConnection("POST", getQueryBaseURI().resolve(requestId+"/status/"+status.name()));
		c.setRequestProperty("Date", Util.formatHttpDate(date));
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
		URL url = new URL("https://blue.at.struktu.ro/idm/lala/");
		HttpsURLConnection c = (HttpsURLConnection)url.openConnection();
		c.setSSLSocketFactory(sc.getSocketFactory());
		
	}
}
