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
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import javax.xml.bind.DataBindingException;
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
	
	private HttpClient client;

	protected List<T> listeners;

	protected Charset defaultCharset;


	protected AbstractBrokerClient() {
		this.defaultCharset = StandardCharsets.UTF_8;
		
		// lazy initialization of HTTP client before first use. 
		// this allows the auth filter (which is set later) 
		// to modify the client builder
		this.client = null;

		this.listeners = new CopyOnWriteArrayList<>();
	}

	private void lazyInitHttpClient() throws IOException {
		if( client != null ) {
			return;
		}
		HttpClient.Builder builder = HttpClient.newBuilder().version(Version.HTTP_1_1);
		if( authFilter != null ) {
			authFilter.configureHttpClient(builder);
		}
		client = builder.build();
	}

	/**
	 * Add a notification listener.
	 * @param listener
	 */
	public void addListener(T listener) {
		listeners.add(listener);
	}
	abstract protected URI getQueryBaseURI();
	abstract protected String getWebsocketPath();

	public WebSocket connectWebsocket() throws IOException{
		connectWebsocket(getWebsocketPath());
		return this.getWebsocket();
	}

	
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

	protected <U> HttpResponse<U> sendRequest(HttpRequest request, BodyHandler<U> handler) throws IOException{
		lazyInitHttpClient();
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
		if( this.notifier == null ) {
			this.notifier = new WebsocketNotificationService(Executors.newSingleThreadExecutor()) {
				@Override
				protected void notifyText(String text) {
					onWebsocketText(text);
				}
				
				@Override
				protected void notifyClose(int statusCode) {
					// before notification, make sure websocket and notifiers are fully closed 
					closeWebsocket();
					onWebsocketClose(statusCode);
				}
			};
		}
		try {
			this.websocket = openWebsocket(urlspec, this.notifier);
		}catch( IOException e ) {
			// keep notifier, even if the connection failed
			throw e;
		}
	}
	public void closeWebsocket() {
		if( this.websocket == null ) {
			// already closed
			return;
		}
		this.websocket.abort();
		this.websocket = null;	
		// keep notifier, even if websocket is closed. notifier might be needed for reconnect
	}

	private WebSocket openWebsocket(String urlspec, WebSocket.Listener listener) throws IOException {
		lazyInitHttpClient();
		WebSocket.Builder wsb = client.newWebSocketBuilder();
		// connection timeout of 5 seconds
		wsb.connectTimeout(Duration.ofSeconds(10));
		// apply authentication filter if available
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
				// will also include java.net.http.HttpTimeoutException
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

	protected <U> U sendAndExpectJaxb(HttpRequest req, Class<U> type) throws IOException {
		HttpResponse<Supplier<U>> resp;
		resp = sendRequest(req, JaxbBodyHandler.forType(type));
		if( resp.statusCode() == HTTP_STATUS_404_NOT_FOUND ) {
			return null;
		}else if( resp.statusCode() == 400 ) {
			throw new IOException("HTTP response status bad request");
		}
		try {
			return resp.body().get();			
		}catch( DataBindingException e ) {
			// this happens if the server provides 200 status with non-xml content
			// e.g., when a proxy or server is mis-configured.
			throw new IOException("Unable retrieve response object", e);
		}

	}

	protected void sendAndExpectStatus(HttpRequest req, int expectedStatus) throws IOException {
		int responseCode;
		responseCode = sendRequest(req, BodyHandlers.discarding()).statusCode();
		if( responseCode != expectedStatus ) {
			throw new IOException("Unexpected response code "+responseCode+" instead of expected "+expectedStatus);
		}
	}

}
