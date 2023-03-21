package org.aktin.broker;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.Provider;

import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.AuthRole;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

@Authenticated
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyAuth extends HttpBearerAuthentication{
	public static final String CLIENT_1_KEY = "X2jdj20xjvV";
	public static final String CLIENT_1_DN = "CN=Test Client 1,L=Test,O=Test";


	@Override
	protected AuthInfo lookupAuthInfo(String token) throws IOException {
		if( token.equals(CLIENT_1_KEY) ){
			return new AuthInfoImpl(CLIENT_1_KEY, CLIENT_1_DN, AuthRole.ALL_NODE);
		}
		return null;
	}

}
