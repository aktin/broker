package org.aktin.broker.client2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.BrokerClientImpl.OutputWriter;
import org.aktin.broker.client.ResponseWithMetadata;
import org.aktin.broker.client.Utils;
import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.util.Util;
import org.w3c.dom.Document;


public class BrokerClient2 extends AbstractBrokerClient<ClientNotificationListener> implements BrokerClient{

	public BrokerClient2(URI endpointURI) {
		super();
		setEndpoint(endpointURI);
	}
//	// aggregator functions
//	private void putResource(String urispec, String contentType, OutputWriter writer) throws IOException{
//		HttpRequest req = createRequest(urispec)
//				.header("Content-Type", contentType)
//				.PUT(BodyPublishers.ofInputStream(streamSupplier)).build();
//		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);		
//		HttpURLConnection c = openConnection("PUT", urispec);
//		c.setDoOutput(true);
//		c.setRequestProperty("Content-Type", contentType);
//		try( OutputStream out = c.getOutputStream() ){
//			writer.write(out);
//		}
//		c.getInputStream().close();
//	}
	@Override
	protected URI getQueryBaseURI() {
		return resolveBrokerURI("my/request/");
	}


	@Override
	public List<RequestInfo> listMyRequests() throws IOException{
		HttpRequest req = createBrokerRequest("my/request").GET().build();
		RequestList resp = sendAndExpectJaxb(req, RequestList.class);
		return postprocessRequestList(resp);
	}
	@Override
	public void postSoftwareVersions(Map<String,String> softwareVersions) throws IOException, NullPointerException{
		Properties map = new Properties();
		map.putAll(softwareVersions);
		putMyResourceProperties("versions", map);
	}
	@Override
	public RequestInfo getRequestInfo(int id) throws IOException {
		HttpRequest req = createBrokerRequest("my/request/"+id).method("OPTIONS", BodyPublishers.noBody()).build();
		return sendAndExpectJaxb(req, RequestInfo.class);
	}
	@Override
	public void deleteMyRequest(int id) throws IOException {
		HttpRequest req = createBrokerRequest("my/request/"+id).DELETE().build();
		HttpResponse<Void> resp = sendRequest(req, BodyHandlers.discarding());
		switch( resp.statusCode() ) {
		case HTTP_STATUS_404_NOT_FOUND:
			// not found is also ok, since the request is not there anymore
		case HTTP_STATUS_204_NO_CONTENT:
			// success
			break;
		default:
			throw new IOException("Unexpected HTTP response code "+resp.statusCode()+" instead of "+HTTP_STATUS_204_NO_CONTENT);
		}
	}

	private HttpRequest createRequestForRequest(int id, String mediaType) throws IOException{
		HttpRequest.Builder rb = createBrokerRequest("my/request/"+id).GET();
		if( mediaType != null ) {
			rb.header("Accept", mediaType);
		}
		return rb.build();
	}
	@Override
	public Reader getMyRequestDefinitionReader(int id, String mediaType) throws IOException {
		HttpResponse<InputStream> resp = getMyRequestDefinition(id, mediaType, BodyHandlers.ofInputStream());
		// check response status, e.g. 404 or 406
		if( resp.statusCode() == 404 ) {
			return null;
		}
		String contentType = resp.headers().firstValue("Content-Type").orElse(null);
		return Utils.contentReaderForInputStream(resp.body(), contentType, defaultCharset);
	}
	@Override
	public String[] getMyRequestDefinitionLines(int id, String mediaType) throws IOException {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public Document getMyRequestDefinitionXml(int id, String mediaType) throws IOException {
		HttpResponse<InputStream> resp = getMyRequestDefinition(id, mediaType, BodyHandlers.ofInputStream());
		if( resp.statusCode() == HTTP_STATUS_404_NOT_FOUND ) {
			return null;
		}
		return Util.parseDocument(resp.body());
	}
	public Path getMyRequestDefinition(int id, String mediaType, Path dest, OpenOption...openOptions) throws MediaTypeNotAcceptableException, IOException {
		HttpResponse<Path> resp = getMyRequestDefinition(id, mediaType, BodyHandlers.ofFile(dest, openOptions));
		if( resp.statusCode() == HTTP_STATUS_404_NOT_FOUND ) {
			return null;
		}else if( resp.statusCode() == HTTP_STATUS_406_NOT_ACCEPTABLE ) {
			throw new MediaTypeNotAcceptableException(mediaType);
		}else if( resp.statusCode() != 200 ) {
			throw new IOException("Request retrieval failed with status code "+resp.statusCode());
		}else {
			return resp.body();
		}
	}
	
	public <T> HttpResponse<T> getMyRequestDefinition(int id, String mediaType, BodyHandler<T> handler) throws IOException {
		HttpRequest req = createRequestForRequest(id, mediaType);
		return sendRequest(req, handler);
	}
	@Override
	public String getMyRequestDefinitionString(int id, String mediaType) throws MediaTypeNotAcceptableException, IOException {
		HttpResponse<String> resp = getMyRequestDefinition(id, mediaType, BodyHandlers.ofString());
		if( resp.statusCode() == HTTP_STATUS_404_NOT_FOUND ) {
			return null;
		}else if( resp.statusCode() == HTTP_STATUS_406_NOT_ACCEPTABLE ) {
			throw new MediaTypeNotAcceptableException(mediaType);
		}
		return resp.body();
	}

	@Override
	public Node getMyNode() throws IOException{
		HttpRequest req = createBrokerRequest("my/node").GET().build();
		return sendAndExpectJaxb(req, Node.class);
	}
	@Override
	@Deprecated
	public void putMyResource(String name, String contentType, final InputStream content) throws IOException{
		HttpRequest req = createRequestForNodeResource(name)
				.header("Content-Type", contentType)
				// TODO request might be repeated, so this method shold be deprecated as inputstream is not repeatable
				.PUT(BodyPublishers.ofInputStream(singleSupplier(content))).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);		
	}
	@Override
	public void putMyResourceXml(String name, Object jaxbObject) throws IOException{
		HttpRequest req = putJAXB(createRequestForNodeResource(name), jaxbObject).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}
	@Override
	public void putMyResourceProperties(String name, Properties properties) throws IOException{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		properties.storeToXML(os, name, StandardCharsets.UTF_8);
		HttpRequest req = createRequestForNodeResource(name)
				.header("Content-Type", "application/xml; charset=utf-8")
				.PUT(BodyPublishers.ofByteArray(os.toByteArray())).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}
	public <T> HttpResponse<T> getMyResource(String name, BodyHandler<T> handler) throws IOException {
		HttpRequest req = createRequestForNodeResource(name).GET().build();
		return sendRequest(req, handler);
	}

	@Override
	public Properties getMyResourceProperties(String name) throws IOException{
		Properties props = null;
		try( InputStream response = getMyResource(name, BodyHandlers.ofInputStream()).body() ){
			props = new Properties();
			props.loadFromXML(response);
		}
		return props;
	}
	private HttpRequest.Builder createRequestForNodeResource(String name)throws IOException{
		return createBrokerRequest("my/node/"+URLEncoder.encode(name, StandardCharsets.UTF_8));
	}
	@Override
	public ResponseWithMetadata getMyResource(String name) throws IOException{
		return wrapResource(getMyResource(name, BodyHandlers.ofInputStream()), name);
	}
	@Override
	public void putMyResource(String name, String contentType, String content) throws IOException{
		HttpRequest req = createRequestForNodeResource(name)
				.header(CONTENT_TYPE_HEADER, contentType+"; charset=UTF-8")
				.PUT(BodyPublishers.ofString(content)).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);		
	}
	@Override
	public void deleteMyResource(String name) throws IOException {
		HttpRequest req = createRequestForNodeResource(name).DELETE().build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

	@Override
	public void putRequestResult(int requestId, String contentType, OutputWriter writer) throws IOException {
		throw new IllegalStateException("Not yet implemented");
	}
	@Override
	public void putRequestResult(int requestId, String contentType, InputStream content) throws IOException {
		HttpRequest req = createAggregatorRequest("my/request/"+requestId+"/result")
				.header(CONTENT_TYPE_HEADER, contentType)
				.PUT(BodyPublishers.ofInputStream( () -> content ))
				.build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);		
	}
	@Override
	public void putRequestResult(int requestId, String contentType, String content) throws IOException {
		HttpRequest req = createAggregatorRequest("my/request/"+requestId+"/result")
				.header(CONTENT_TYPE_HEADER, contentType)
				.PUT(BodyPublishers.ofString(content, StandardCharsets.UTF_8))
				.build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}
	@Override
	public void postRequestStatus(int requestId, RequestStatus status) throws IOException {
		postRequestStatus(requestId, status, null, null);	
	}

	@Override
	public void postRequestFailed(int requestId, String message, Throwable throwable) throws IOException{
		// concatenate message with stacktrace
		StringBuilder b = new StringBuilder();
		if( message != null ) {
			b.append(message);
		}
		if( throwable != null ) {
			if( b.length() > 0 ) {
				b.append('\n');
			}
			// append stack trace
			try( StringWriter w = new StringWriter();
					PrintWriter p = new PrintWriter(w) ){
				throwable.printStackTrace(p);
				b.append(w.toString());
			}
		}
		postRequestStatus(requestId, RequestStatus.failed, null, b.toString());	
	}
	@Override
	public void postRequestStatus(int requestId, RequestStatus status, Instant date, String description) throws IOException{
		HttpRequest.Builder rb = createBrokerRequest("my/request/"+requestId+"/status/"+status.name());
		if( date != null ) {
			rb.header("Date", Util.formatHttpDate(date));			
		}
		if( description != null ) {
			rb.header("Content-Type", "text/plain; charset=UTF-8");
			rb.POST(BodyPublishers.ofString(description));			
		}else {
			rb.POST(BodyPublishers.noBody());
		}
		
		HttpRequest req = rb.build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

	@Override
	public String getWebsocketPath() {
		return "my/websocket";
	}
//	public WebSocket openWebsocket(ClientNotificationListener listener) throws IOException {
//		return super.openWebsocket("my/websocket", new WebsocketClientListener(listener));
//	}
	@Override
	public BrokerStatus getBrokerStatus() throws IOException {
		HttpRequest req = createBrokerRequest("status").GET().build();
		return sendAndExpectJaxb(req, BrokerStatus.class);
	}
	@Override
	protected void onWebsocketText(String text) {
		int sep = text.indexOf(' ');
		String command = text.substring(0, sep);
		String arg = text.substring(sep+1);
		switch( command ) {
		case "published":
			for( ClientNotificationListener listener : listeners )
			listener.onRequestPublished(Integer.valueOf(arg));
			break;
		case "closed":
			for( ClientNotificationListener listener : listeners )
			listener.onRequestClosed(Integer.valueOf(arg));
			break;
		default:
			// ignoring unsupported websocket command
			// TODO log warning
		}
	}



}
