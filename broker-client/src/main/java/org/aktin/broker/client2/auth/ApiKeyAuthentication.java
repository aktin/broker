package org.aktin.broker.client2.auth;

import java.io.IOException;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApiKeyAuthentication extends HttpAuthorizationBearerAuth{
	private String apiKey;

	@Override
	protected String getBearerToken() throws IOException {
		return apiKey;
	}

}
