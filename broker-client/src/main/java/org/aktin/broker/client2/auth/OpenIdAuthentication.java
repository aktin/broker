package org.aktin.broker.client2.auth;

import java.io.IOException;
import org.keycloak.authorization.client.AuthzClient;

public class OpenIdAuthentication extends HttpAuthorizationBearerAuth{
	private String apiKey;

	public OpenIdAuthentication(String apiKey) {
		this.apiKey = apiKey;
	}

	@Override
	protected String getBearerToken() throws IOException {
		return AuthzClient.create().obtainAccessToken().getToken();
	}

}
