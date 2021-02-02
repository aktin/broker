package org.aktin.broker.auth.cred;

import java.io.IOException;

import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.HttpBearerAuthentication;
public class CredentialTokenAuth extends HttpBearerAuthentication{
	private TokenManager manager;

	public CredentialTokenAuth(TokenManager manager) {
		this.manager = manager;
	}

	private static String getClientDn(Token token) {
		return "CN="+token.getName()+",OU=admin";
	}
	@Override
	protected AuthInfo lookupAuthInfo(String token) throws IOException {
		Token t = manager.lookupToken(token);
		if( t == null ) {
			// unauthorized
			return null;
		}
		t.renew();
		String clientDn = getClientDn(t);
		return new AuthInfoImpl(t.getName(), clientDn, defaultRolesForClientDN(clientDn));
	}


}
