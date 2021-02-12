package org.aktin.broker.client2;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.WebSocket;
import java.net.http.HttpClient.Version;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

public class AbstractBrokerClient {
	protected static final int HTTP_STATUS_204_NO_CONTENT = 204;
	protected static final String CONTENT_TYPE_HEADER = "Content-type";

	private URI brokerEndpoint;
	private URI aggregatorEndpoint;
	private AuthFilter authFilter;

	protected HttpClient client;
	protected Charset defaultCharset;

	protected AbstractBrokerClient() {
		this.defaultCharset = StandardCharsets.UTF_8;
		this.client = HttpClient.newHttpClient();
	}

	protected URI getQueryBaseURI() {
		return resolveBrokerURI("my/request/");
	}
	protected URI resolveBrokerURI(String spec){
		return brokerEndpoint.resolve(spec);
	}

	public void setAuthFilter(AuthFilter auth) {
		this.authFilter = auth;
	}

	public void setAggregatorEndpoint(URI uri) {
		this.aggregatorEndpoint = uri;
	}

	public void setEndpoint(URI endpointURI) {
		this.brokerEndpoint = endpointURI;
		setAggregatorEndpoint(brokerEndpoint.resolve("../aggregator/"));
	}
//	private final HttpRequest createRequest(String method, String spec, BodyPublisher publisher) throws IOException{
//		HttpRequest.Builder builder = createRequest(spec);
//		return builder.method(method, publisher).build();
//	}
	
	private HttpRequest.Builder createRequest(URI base, String urispec) throws IOException{
		HttpRequest.Builder builder = HttpRequest.newBuilder();
		URI uri = base.resolve(urispec);
		builder.uri(uri).version(Version.HTTP_1_1);
		if( authFilter != null ) {
			authFilter.addAuthentication(builder);
		}
		return builder;
	}
	protected HttpRequest.Builder createBrokerRequest(String urispec) throws IOException{
		return createRequest(brokerEndpoint, urispec);
	}
	protected HttpRequest.Builder createAggregatorRequest(String urispec) throws IOException{
		return createRequest(aggregatorEndpoint, urispec);
	}
	protected WebSocket openWebsocket(String urlspec, WebsocketListener listener) throws IOException {
		WebSocket.Builder wsb = client.newWebSocketBuilder();
		if( authFilter != null ) {
			authFilter.addAuthentication(wsb);
		}
		try {
			URI wsuri = new URI("ws", brokerEndpoint.resolve(urlspec).getRawSchemeSpecificPart(), null);
			System.err.println("Websocket target: "+wsuri.toString());
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

}
