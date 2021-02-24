package org.aktin.broker.client2.auth;

import java.io.IOException;

public class ApiKeyAuthentication extends HttpAuthorizationBearerAuth{
	private String apiKey;

	public ApiKeyAuthentication(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	protected String getBearerToken() throws IOException {
		return apiKey;
	}

}
