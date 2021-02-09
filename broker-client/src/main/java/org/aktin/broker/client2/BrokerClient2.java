package org.aktin.broker.client2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.xml.bind.JAXB;

import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.BrokerClientImpl.OutputWriter;
import org.aktin.broker.client.ResourceMetadata;
import org.aktin.broker.client.Utils;
import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.util.Util;
import org.w3c.dom.Document;


public class BrokerClient2 implements BrokerClient{
	private URI endpointUri;
	private HttpClient client;
	private Charset defaultCharset;
	private AuthFilter authFilter;

	private static final int HTTP_STATUS_204_NO_CONTENT = 204;
	
	protected URI getQueryBaseURI() {
		return resolveBrokerURI("my/request/");
	}
	protected URI resolveBrokerURI(String spec){
		return endpointUri.resolve(spec);
	}

	public BrokerClient2(URI endpointURI) {
		this.defaultCharset = StandardCharsets.UTF_8;
		this.endpointUri = endpointURI;
		this.client = HttpClient.newHttpClient();
		// TODO auth filter
	}
//	private final HttpRequest createRequest(String method, String spec, BodyPublisher publisher) throws IOException{
//		HttpRequest.Builder builder = createRequest(spec);
//		return builder.method(method, publisher).build();
//	}
	
	protected HttpRequest.Builder createRequest(String urispec) throws IOException{
		HttpRequest.Builder builder = HttpRequest.newBuilder();
		builder.uri(endpointUri.resolve(urispec));
		if( authFilter != null ) {
			authFilter.addAuthentication(builder);
		}
		return builder;
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

	protected List<RequestInfo> postprocessRequestList(RequestList list) throws IOException{
		if( list == null ){
			throw new IOException("Unmarshalling of request list failed");
		}
		if( list.getRequests() != null ){
			return list.getRequests();
		}else{
			return Collections.emptyList();
		}		
	}
	private <T> T sendAndExpectJaxb(HttpRequest req, Class<T> type) throws IOException {
		HttpResponse<T> resp;
		try {
			resp = client.send(req, JaxbBodyHandler.forType(type));
		} catch (InterruptedException e) {
			throw new IOException("HTTP connection interrupted",e);
		}
		return resp.body();
	}
	private void sendAndExpectStatus(HttpRequest req, int expectedStatus) throws IOException {
		int responseCode;
		try {
			responseCode = client.send(req, BodyHandlers.discarding()).statusCode();
		} catch (InterruptedException e) {
			throw new IOException("HTTP connection interrupted",e);
		}
		if( responseCode != expectedStatus ) {
			throw new IOException("Unexpected response code "+responseCode+" instead of expected "+expectedStatus);
		}
	}

	@Override
	public List<RequestInfo> listMyRequests() throws IOException{
		HttpRequest req = createRequest("my/request").GET().build();
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
		HttpRequest req = createRequest("my/request/"+id).method("OPTIONS", BodyPublishers.noBody()).build();
		return sendAndExpectJaxb(req, RequestInfo.class);
	}
	@Override
	public void deleteMyRequest(int id) throws IOException {
		HttpRequest req = createRequest("my/request/"+id).DELETE().build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

	private HttpRequest createRequestForRequest(int id, String mediaType) throws IOException{
		HttpRequest.Builder rb = createRequest("my/request/"+id).GET();
		if( mediaType != null ) {
			rb.header("Accept", mediaType);
		}
		return rb.build();
	}
	@Override
	public Reader getMyRequestDefinitionReader(int id, String mediaType) throws IOException {
		HttpResponse<InputStream> resp = getMyRequestDefinition(id, mediaType, BodyHandlers.ofInputStream());
		// TODO check response status, e.g. 404 or 406
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
		return Util.parseDocument(resp.body());
	}
	
	public <T> HttpResponse<T> getMyRequestDefinition(int id, String mediaType, BodyHandler<T> handler) throws IOException {
		HttpRequest req = createRequestForRequest(id, mediaType);
		try {
			return client.send(req, handler);
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}
	@Override
	public String getMyRequestDefinitionString(int id, String mediaType) throws IOException {
		return getMyRequestDefinition(id, mediaType, BodyHandlers.ofString()).body();
	}

	@Override
	public Node getMyNode() throws IOException{
		HttpRequest req = createRequest("my/node").GET().build();
		return sendAndExpectJaxb(req, Node.class);
	}
	private static <T> Supplier<T> singleSupplier(T item){
		return new Supplier<T>() {
			private boolean supplied;
			@Override
			public T get() {
				if( supplied ) {
					return null;
				}
				this.supplied = true;
				return item;
			}
		};
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
		StringWriter w = new StringWriter();
		JAXB.marshal(jaxbObject, w);
		
		HttpRequest req = createRequestForNodeResource(name)
				.header("Content-Type", "application/xml; charset=utf-8")
				.PUT(BodyPublishers.ofString(w.toString(), StandardCharsets.UTF_8)).build();
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
		try {
			return client.send(req, handler);
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
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
		return createRequest("my/node/"+URLEncoder.encode(name, StandardCharsets.UTF_8));
	}
	protected static ResourceMetadata wrapResource(HttpResponse<InputStream> resp, String name) throws IOException{
		if( resp.statusCode() == 404 ){
			return null;
		}else if( resp.statusCode() != 200 ){
			throw new IOException("Unexpected HTTP response code "+resp.statusCode());
		}
		return new ResourceMetadataResponseWrapper(name, resp);
	}
	@Override
	public ResourceMetadata getMyResource(String name) throws IOException{
		return wrapResource(getMyResource(name, BodyHandlers.ofInputStream()), name);
	}
	@Override
	public void putMyResource(String name, String contentType, String content) throws IOException{
		HttpRequest req = createRequestForNodeResource(name)
				.header("Content-Type", contentType+"; charset=UTF-8")
				.PUT(BodyPublishers.ofString(content)).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);		
	}
	@Override
	public void deleteMyResource(String name) throws IOException {
		HttpRequest req = createRequest("my/request").DELETE().build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);
	}

	@Override
	public void putRequestResult(int requestId, String contentType, OutputWriter writer) throws IOException {
		throw new IllegalStateException("Not yet implemented");
	}
	@Override
	public void putRequestResult(int requestId, String contentType, InputStream content) throws IOException {
		HttpRequest req = createRequest("my/request").PUT(BodyPublishers.ofInputStream( () -> content )).build();
		sendAndExpectStatus(req, HTTP_STATUS_204_NO_CONTENT);		
	}
	@Override
	public void putRequestResult(int requestId, String contentType, String content) throws IOException {
		HttpRequest req = createRequest("my/request").PUT(BodyPublishers.ofString(content, StandardCharsets.UTF_8)).build();
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
			if( !b.isEmpty() ) {
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
		HttpRequest.Builder rb = createRequest("my/request/"+requestId+"/status/"+status.name());
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

	public CompletableFuture<WebSocket> openWebsocket(NotificationListener listener) throws IOException {
		WebSocket.Builder wsb = client.newWebSocketBuilder();
		if( authFilter != null ) {
			authFilter.addAuthentication(wsb);
		}
		return wsb.buildAsync(endpointUri.resolve("my/websocket"), new WebsocketListener(listener));
	}
	@Override
	public BrokerStatus getBrokerStatus() throws IOException {
		HttpRequest req = createRequest("status").GET().build();
		return sendAndExpectJaxb(req, BrokerStatus.class);
	}

}
