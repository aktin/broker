package org.aktin.broker.client2;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import javax.xml.bind.JAXB;

import org.aktin.broker.client.ResponseWithMetadata;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestList;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractBrokerClient<T extends NotificationListener> {
	protected static final int HTTP_STATUS_204_NO_CONTENT = 204;
	protected static final int HTTP_STATUS_201_CREATED = 201;
	protected static final int HTTP_STATUS_404_NOT_FOUND = 404;
	protected static final int HTTP_STATUS_406_NOT_ACCEPTABLE = 406;
	protected static final String LAST_MODIFIED_HEADER = "Last-Modified";
	protected static final String CONTENT_TYPE_HEADER = "Content-type";
	protected static final String ACCEPT_HEADER = "Accept";
	protected static final String LOCATION_HEADER = "Location";
	protected static final String CONTENTMD5_HEADER = "Content-MD5";
	protected static final String ETAG_HEADER = "ETag";
	protected static final String MEDIATYPE_APPLICATION_XML_UTF8 = "application/xml; charset=utf-8";

	private URI brokerEndpoint;
	@Getter
	@Setter
	private URI aggregatorEndpoint;
	@Setter
	@Getter
	private AuthFilter authFilter;
	
	@Getter
	private WebSocket websocket;
	private WebsocketNotificationService notifier;
	
	protected List<T> listeners;

	protected HttpClient client;
	protected Charset defaultCharset;


	protected AbstractBrokerClient() {
		this.defaultCharset = StandardCharsets.UTF_8;
		this.client = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
		this.listeners = new CopyOnWriteArrayList<>();
	}

	public void addListener(T listener) {
		listeners.add(listener);
	}
	abstract protected URI getQueryBaseURI();
	protected URI resolveBrokerURI(String spec){
		return brokerEndpoint.resolve(spec);
	}

	public void setEndpoint(URI endpointURI) {
		this.brokerEndpoint = endpointURI;
		setAggregatorEndpoint(brokerEndpoint.resolve("../aggregator/"));
	}
//	private final HttpRequest createRequest(String method, String spec, BodyPublisher publisher) throws IOException{
//		HttpRequest.Builder builder = createRequest(spec);
//		return builder.method(method, publisher).build();
//	}

	protected static ResponseWithMetadata wrapResource(HttpResponse<InputStream> resp, String name) throws IOException{
		if( resp.statusCode() == 404 ){
			return null;
		}else if( resp.statusCode() != 200 ){
			throw new IOException("Unexpected HTTP response code "+resp.statusCode());
		}
		return new ResourceMetadataResponseWrapper(name, resp);
	}

	protected static <T> Supplier<T> singleSupplier(T item){
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

	protected <T> HttpResponse<T> sendRequest(HttpRequest request, BodyHandler<T> handler) throws IOException{
		try {
			return client.send(request, handler);
		} catch (InterruptedException e) {
			throw new IOException("HTTP communication interruped");
		}		
	}
	private HttpRequest.Builder createRequest(URI base, String urispec) throws IOException{
		HttpRequest.Builder builder = HttpRequest.newBuilder();
		URI uri = base.resolve(urispec);
		builder.uri(uri).version(Version.HTTP_1_1);
		if( authFilter != null ) {
			authFilter.addAuthentication(builder);
		}
		return builder;
	}
	protected HttpRequest.Builder putJAXB(HttpRequest.Builder builder, Object jaxbObject){
		StringWriter w = new StringWriter();
		JAXB.marshal(jaxbObject, w);
		
		return builder
				.header(CONTENT_TYPE_HEADER, MEDIATYPE_APPLICATION_XML_UTF8)
				.PUT(BodyPublishers.ofString(w.toString(), StandardCharsets.UTF_8));
	}
	protected HttpRequest.Builder createBrokerRequest(String urispec) throws IOException{
		return createRequest(brokerEndpoint, urispec);
	}
	protected HttpRequest.Builder createAggregatorRequest(String urispec) throws IOException{
		return createRequest(aggregatorEndpoint, urispec);
	}
	
	/**
	 * This method will be called by threads created from {@link #notifier}
	 * @param statusCode websocket close status code
	 */
	protected void onWebsocketClose(int statusCode) {
		listeners.forEach(l -> l.onWebsocketClosed(statusCode));
	}
	protected abstract void onWebsocketText(String text);

	protected void connectWebsocket(String urlspec) throws IOException {
		if( this.websocket != null ) {
			throw new IOException("Websocket already connected");
		}
		this.notifier = new WebsocketNotificationService(Executors.newSingleThreadExecutor()) {
			@Override
			protected void notifyText(String text) {
				onWebsocketText(text);
			}
			
			@Override
			protected void notifyClose(int statusCode) {
				onWebsocketClose(statusCode);
			}
		};
		try {
			this.websocket = openWebsocket(urlspec, this.notifier);
		}catch( IOException e ) {
			this.notifier.shutdown();
			this.notifier = null;
			throw e;
		}
	}
	public void closeWebsocket() {
		if( this.websocket == null ) {
			// already closed
			return;
		}
		this.notifier.shutdown();
		this.websocket.abort();
		this.notifier = null;
		this.websocket = null;	
	}

	private WebSocket openWebsocket(String urlspec, WebSocket.Listener listener) throws IOException {
		WebSocket.Builder wsb = client.newWebSocketBuilder();
		if( authFilter != null ) {
			authFilter.addAuthentication(wsb);
		}
		String scheme;
		switch( brokerEndpoint.getScheme() ) {
		case "http":
			scheme = "ws"; break;
		case "https":
			scheme = "wss"; break;
		default:
			throw new IOException("Websocket connection requires http or https scheme in broker URI");
		};
		
		try {
			URI wsuri = new URI(scheme, brokerEndpoint.resolve(urlspec).getRawSchemeSpecificPart(), null);
			return wsb.buildAsync(wsuri, listener).get();
		} catch (InterruptedException e ) {
			throw new IOException("Websocket open operation interrupted", e);
		} catch( ExecutionException e ) {
			if( e.getCause() instanceof IOException ) {
				throw (IOException)e.getCause();
			}else {
				throw new IOException("Websocket connection failed",e.getCause());
			}
		} catch (URISyntaxException e) {
			throw new IOException("Synstax error during URI construction",e);
		}
	}

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

	protected <T> T sendAndExpectJaxb(HttpRequest req, Class<T> type) throws IOException {
		HttpResponse<Supplier<T>> resp;
		try {
			resp = client.send(req, JaxbBodyHandler.forType(type));
		} catch (InterruptedException e) {
			throw new IOException("HTTP connection interrupted",e);
		}
		if( resp.statusCode() == HTTP_STATUS_404_NOT_FOUND ) {
			return null;
		}
		return resp.body().get();
	}

	protected void sendAndExpectStatus(HttpRequest req, int expectedStatus) throws IOException {
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

}
