package org.aktin.broker.client.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpApiKeyAuth implements ClientAuthenticator {

	private String httpAuthValue;

	private HttpApiKeyAuth(String headerValue){
		this.httpAuthValue = headerValue;
	}

	public static HttpApiKeyAuth newBearer(String key){
		return new HttpApiKeyAuth("Bearer "+key);
	}
	@Override
	public HttpURLConnection openAuthenticatedConnection(URL url) throws IOException {
		HttpURLConnection c = (HttpURLConnection)url.openConnection();
		c.setRequestProperty("Authorization", httpAuthValue);
		return c;
	}

}
