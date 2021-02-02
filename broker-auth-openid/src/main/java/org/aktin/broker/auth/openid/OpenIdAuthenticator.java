package org.aktin.broker.auth.openid;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.HeaderAuthentication;

public class OpenIdAuthenticator implements HeaderAuthentication{
	private OpenIdConfig config;
	
	public OpenIdAuthenticator(OpenIdConfig config) {
		this.config = config;
	}
	@Override
	public AuthInfo authenticateByHeaders(Function<String, String> getHeader) throws IOException {
		Objects.requireNonNull(this.config);
		// TODO use getHeader to obtain token
		// TODO validate token
		// TODO retrieve details from token and fill and return AuthInfo (e.g. AuthInfoImpl)
		return null;
	}

}
