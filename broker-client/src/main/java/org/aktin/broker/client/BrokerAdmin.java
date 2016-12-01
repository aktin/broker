package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.activation.DataSource;
import javax.xml.bind.JAXB;

import org.aktin.broker.client.BrokerClient.OutputWriter;
import org.aktin.broker.xml.NodeList;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.RequestStatusList;
import org.aktin.broker.xml.ResultInfo;
import org.aktin.broker.xml.ResultList;
import org.w3c.dom.Node;

public class BrokerAdmin extends AbstractBrokerClient {

	public BrokerAdmin(URI brokerEndpoint) {
		super(brokerEndpoint);
	}
	@Override
	public void setEndpoint(URI brokerEndpoint){
		super.setEndpoint(brokerEndpoint);
		// TODO retrieve aggregator URL from broker info
		setAggregatorEndpoint(brokerEndpoint.resolve("/aggregator/"));		
	}
	@Override
	protected URI getQueryBaseURI() {
		return resolveBrokerURI("request/");
	}
	public Reader getRequestDefinition(String id, String mediaType) throws IOException{
		return getRequestDefinition(getQueryURI(id), mediaType);
	}
	public Reader getRequestDefinition(URI location, String mediaType) throws IOException{
		HttpURLConnection c = openConnection("GET", location);
		return contentReader(c, mediaType);	
		
	}
	public URI createRequest(String contentType, OutputWriter writer) throws IOException{
		HttpURLConnection c = openConnection("POST", "request");
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", contentType);
		try( OutputStream out = c.getOutputStream() ){
			writer.write(out);
		}
		c.getInputStream().close();
		String location = c.getHeaderField("Location");
		if( location == null ){
			throw new IOException("No location in response headers");
		}
		try {
			return new URI(location);
		} catch (URISyntaxException e) {
			throw new IOException("Response header location no valid URI", e);
		}
	}
	
	public URI createRequest(String contentType, final InputStream content) throws IOException{
		return createRequest(contentType,  new OutputWriter(){
			@Override
			public void write(OutputStream dest) throws IOException {
				writeContent(dest, content);
			}	
		});
	}
	public URI createRequest(String contentType, final Node content) throws IOException{
		return createRequest(contentType+";charset=UTF-8", new OutputWriter(){
			@Override
			public void write(OutputStream dest) throws IOException {
				writeContent(content, dest, "UTF-8");
			}	
		});
	}
	public URI createRequest(String contentType, String content) throws IOException{
		return createRequest(contentType+";charset=UTF-8", new OutputWriter.ForString(content, "UTF-8"));
	}
	public void putRequestDefinition(URI requestURI, String contentType, OutputWriter writer) throws IOException{
		HttpURLConnection c = openConnection("PUT", requestURI);
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", contentType);
		try( OutputStream out = c.getOutputStream() ){
			writer.write(out);
		}
		c.getInputStream().close();		
	}
	public void publishRequest(String requestId) throws IOException{
		URI uri = getQueryURI(requestId);
		HttpURLConnection c = openConnection("POST", uri.resolve(uri.getPath()+"/publish"));
		c.getInputStream().close();
	}
	public void putRequestDefinition(String requestId, String contentType, OutputWriter writer) throws IOException{
		putRequestDefinition(getQueryURI(requestId), contentType, writer);
	}
	public void putRequestDefinition(URI requestURI, String contentType, String content) throws IOException{
		putRequestDefinition(requestURI, contentType+";charset=UTF-8", new OutputWriter.ForString(content, "UTF-8"));
	}

	public List<org.aktin.broker.xml.Node> listNodes() throws IOException{
		HttpURLConnection c = openConnection("GET", "node");
		NodeList nl;
		try( InputStream response = c.getInputStream() ){
			nl = JAXB.unmarshal(response, NodeList.class);
		}
		if( nl.getNodes() == null ){
			return Collections.emptyList();
		}else{
			return nl.getNodes();
		}
	}
	public org.aktin.broker.xml.Node getNode(int nodeId) throws IOException{
		HttpURLConnection c = openConnection("GET", "node/"+nodeId);
		if( c.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND ){
			return null;
		}else try( InputStream response = c.getInputStream() ){
			return JAXB.unmarshal(response, org.aktin.broker.xml.Node.class);
		}
	}
	public List<RequestInfo> listAllRequests() throws IOException{
		HttpURLConnection c = openConnection("GET", "request");
		RequestList list = null;
		try( InputStream response = c.getInputStream() ){
			list = JAXB.unmarshal(response, RequestList.class);
		}
		return postprocessRequestList(list);
	}
	public RequestInfo getRequestInfo(String requestId) throws IOException{
		HttpURLConnection c = openConnection("OPTIONS", "request/"+requestId);
		if( c.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND ){
			return null;
		}else try( InputStream response = c.getInputStream() ){
			return JAXB.unmarshal(response, RequestInfo.class);
		}
	}
	public List<RequestStatusInfo> listRequestStatus(String requestId) throws IOException{
		HttpURLConnection c = openConnection("GET", "request/"+requestId+"/status");
		c.setRequestProperty("Content-Type", "application/xml");
		RequestStatusList list = null;
		try( InputStream response = c.getInputStream() ){
			list = JAXB.unmarshal(response, RequestStatusList.class);
		}
		if( list.getStatusList() == null ){
			return Collections.emptyList();
		}else{
			return list.getStatusList();
		}
	}

	public List<ResultInfo> listResults(String requestId) throws IOException{
		HttpURLConnection c = openConnection("GET", resolveAggregatorURI("request/"+requestId+"/result"));
		c.setRequestProperty("Content-Type", "application/xml");
		ResultList list = null;
		try( InputStream response = c.getInputStream() ){
			list = JAXB.unmarshal(response, ResultList.class);
		}
		if( list.getResults() == null ){
			return Collections.emptyList();
		}else{
			return list.getResults();
		}
	}
	// TODO ResultInfo getResultInfo(String requestId, String nodeId)
	// TODO remove Function<...> for Java 7 compatibility
	public <T> T getResult(String requestId, int nodeId, String acceptMediaType, Function<DataSource,T> unmarshaller) throws IOException{
		HttpURLConnection c = openConnection("GET", resolveAggregatorURI("request/"+requestId+"/result/"+nodeId));
		if( acceptMediaType != null ){
			c.setRequestProperty("Accept", acceptMediaType);
		}
		if( c.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND ){
			return null;
		}
		DataSource ds = new URLConnectionDataSource(c);
		try{
			return unmarshaller.apply(ds);
		}catch( UncheckedIOException e ){
			// unwrap
			throw e.getCause();
		}
	}

	public String getResultString(String requestId, int nodeId, String acceptMediaType) throws IOException{
		// TODO can we use Function in Java7?
		return getResult(requestId, nodeId, acceptMediaType, new Function<DataSource, String>() {

			@Override
			public String apply(DataSource ds) {
				// TODO charset
				try( InputStreamReader reader = new InputStreamReader(ds.getInputStream()) ){
					StringBuilder sb = new StringBuilder();
					char[] buf = new char[2048];
					while( true ){
						int len = reader.read(buf, 0, buf.length);
						if( len == -1 )break;
						sb.append(buf, 0, len);
					}
					return sb.toString();
				}catch( IOException e ){
					throw new UncheckedIOException(e);
				}
			}
		});
	}
}
