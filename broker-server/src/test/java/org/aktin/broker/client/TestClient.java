package org.aktin.broker.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;

import org.aktin.broker.auth.AuthFilterSSLHeaders;

public class TestClient extends BrokerClient{

	private String certId;
	private String clientDn;
	
	public TestClient(URI brokerEndpoint, String certId, String clientDn){
		super(brokerEndpoint);
		this.certId = certId;
		this.clientDn = clientDn;
	}
	@Override
	public HttpURLConnection openConnection(String method, URI uri) throws IOException{
		HttpURLConnection c = super.openConnection(method, uri);
		c.setRequestProperty(AuthFilterSSLHeaders.X_SSL_CLIENT_ID, certId);
		c.setRequestProperty(AuthFilterSSLHeaders.X_SSL_CLIENT_DN, clientDn);
		c.setRequestProperty(AuthFilterSSLHeaders.X_SSL_CLIENT_VERIFY, "SUCCESS");
		return c;
	}
	
	public static void main(String[] args) throws URISyntaxException, IOException{
		TestClient c = new TestClient(new URI("http://localhost:8080/broker/"), "02", "/CN=Test");
		c.listMyRequests();
	}
}
