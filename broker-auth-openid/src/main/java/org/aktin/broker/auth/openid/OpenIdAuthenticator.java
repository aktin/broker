package org.aktin.broker.auth.openid;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.AuthRole;
import org.aktin.broker.server.auth.HeaderAuthentication;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

public class OpenIdAuthenticator implements HeaderAuthentication {

  public static final String CLIENT_ID = "client_id";
  public static final String CLIENT_SECRET = "client_secret";
  public static final String TOKEN = "token";
  public static final String TOKEN_INTROSPECTION_PATH = "protocol/openid-connect/token/introspect";
  public static final String KEY_JWT_USERNAME = "clientId";
  private final OpenIdConfig config;

  public OpenIdAuthenticator(OpenIdConfig config) {
    this.config = config;
  }

  @Override
  public AuthInfo authenticateByHeaders(Function<String, String> getHeader) {
    Objects.requireNonNull(this.config);
    String accessTokenSerialized = HttpBearerAuthentication.extractBearerToken(getHeader.apply(
        HttpHeaders.AUTHORIZATION));

    // Use token introspection endpoint to validate and decode token
    String introspectionResponse = introspectToken(accessTokenSerialized);
    JsonElement jsonElement = JsonParser.parseString(introspectionResponse);

    String siteId = jsonElement.getAsJsonObject().get(KEY_JWT_USERNAME).getAsString();
    String siteName = jsonElement.getAsJsonObject().get(config.getSiteNameClaim()).getAsString();

    // Currently, the definition is that only diz-clients get a site name claim. This might change.
    Set<AuthRole> roles = new HashSet<>();
    if (siteName != null && !siteName.isEmpty()) {
      roles.add(AuthRole.NODE_READ);
      roles.add(AuthRole.NODE_WRITE);
    }

    return new AuthInfoImpl(siteId, "CN=" + siteName, roles);
  }

	/**
	 * Take an access token and check its viability.
	 * @param accessTokenSerialized the serialized access token as received in the Auth header
	 * @return the introspection result string received from the oauth server
	 */
  private String introspectToken(String accessTokenSerialized) {
    Client client = ClientBuilder.newClient();

    WebTarget webTarget
        = client.target(config.getAuth_host());
    WebTarget introspectionWebTarget
        = webTarget.path(TOKEN_INTROSPECTION_PATH);

    MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
    formData.add(CLIENT_ID, config.getClientId());
    formData.add(CLIENT_SECRET, config.getClientSecret());
    formData.add(TOKEN, accessTokenSerialized);

    Response response
        = introspectionWebTarget.request().post(Entity.form(formData));

    return response.readEntity(String.class);
  }

}
