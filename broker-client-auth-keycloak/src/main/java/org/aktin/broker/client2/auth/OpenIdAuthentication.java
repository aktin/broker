package org.aktin.broker.client2.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessTokenResponse;

/**
 * OpenID authentication via Keycloak client.
 * Use e.g. by passing the file {@code keycloak.json} to 
 * constructor {@link #OpenIdAuthentication(String)} 
 *
 */
public class OpenIdAuthentication extends HttpAuthorizationBearerAuth {
	private AuthzClient client;
	private AccessTokenResponse resp;
	private long expirationTimestamp;

	private void checkAndRefreshToken() {
		long now = System.currentTimeMillis();
		if( expirationTimestamp == 0 || expirationTimestamp < now ) {
			// obtain new token
			resp = client.obtainAccessToken();
			// resp.expiresIn is seconds???
			expirationTimestamp = now + 1000*resp.getExpiresIn();		
		}
	}
	public OpenIdAuthentication(String keycloakJsonConfigPath) throws IOException {
		try (InputStream in = Files.newInputStream(Paths.get(keycloakJsonConfigPath))) {
			this.client = AuthzClient.create(in);
			checkAndRefreshToken();
		}
	}

	@Override
	protected String getBearerToken() throws IOException {
		checkAndRefreshToken();
		return resp.getToken();
	}

}
