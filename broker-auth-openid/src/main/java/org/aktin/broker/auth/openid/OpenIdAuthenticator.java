package org.aktin.broker.auth.openid;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.AuthRole;
import org.aktin.broker.server.auth.HeaderAuthentication;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

public class OpenIdAuthenticator implements HeaderAuthentication{

	public static final String CLIENT_ID = "client_id";
	public static final String CLIENT_SECRET = "client_secret";
	public static final String TOKEN = "token";
	public static final String TOKEN_INTROSPECTION_PATH = "protocol/openid-connect/token/introspect";
	private OpenIdConfig config;
	
	public OpenIdAuthenticator(OpenIdConfig config) {
		this.config = config;
	}
	@Override
	public AuthInfo authenticateByHeaders(Function<String, String> getHeader) throws IOException {
		Objects.requireNonNull(this.config);
		String accessTokenSerialized = HttpBearerAuthentication.extractBearerToken(getHeader.apply(
				HttpHeaders.AUTHORIZATION));

		// Use token introspection endpoint to validate and decode token
		Client client = ClientBuilder.newClient();

		WebTarget webTarget
				= client.target(config.getAuth_host());
		WebTarget introspectionWebTarget
				= webTarget.path(TOKEN_INTROSPECTION_PATH);
		Invocation.Builder invocationBuilder
				= introspectionWebTarget.request(MediaType.APPLICATION_JSON);

		MultivaluedMap formData = new MultivaluedHashMap();
		formData.add(CLIENT_ID, config.getClientId());
		formData.add(CLIENT_SECRET, config.getClientSecret());
		formData.add(TOKEN, accessTokenSerialized);

		Response response
				= introspectionWebTarget.request().post(Entity.form(formData));

		String introspectionResponse = response.readEntity(String.class);
		JsonElement jsonElement = JsonParser.parseString(introspectionResponse);

		Set<AuthRole> roles = new HashSet<>();
		roles.add(AuthRole.NODE_READ);
		roles.add(AuthRole.NODE_WRITE);
		// TODO: change keycloak setup to contain everything we need and then use the info from keycloak
//		AuthInfo authInfo = new AuthInfoImpl("foo", "CN=" + jsonElement.getAsJsonObject().get("site-name").getAsString(), roles);
		AuthInfo authInfo = new AuthInfoImpl("foo", "CN=Grey Sloan Memorial Hospital,O=Grey Sloan Memorial Hospital,L=Seattle", roles);
		return authInfo;
	}

}
