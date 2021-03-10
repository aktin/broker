package org.aktin.broker.client2.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.keycloak.authorization.client.AuthzClient;

public class OpenIdAuthentication extends HttpAuthorizationBearerAuth {

  private String apiKey;

  public OpenIdAuthentication(String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  protected String getBearerToken() throws IOException {
    try (InputStream in = Files.newInputStream(Paths.get("keycloak.json"))) {
      return AuthzClient.create(in).obtainAccessToken().getToken();
    } catch (IOException e) {
			System.err.println("Unable to read keycloak.json: " + e.getMessage());
			throw(e);
		}
  }

}
