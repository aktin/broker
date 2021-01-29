package org.aktin.broker;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;

import org.aktin.broker.auth.AuthFilterAPIKeys;
import org.aktin.broker.rest.Authenticated;

@Authenticated
@Provider
@Priority(Priorities.AUTHENTICATION)
public class ApiKeyAuth extends AuthFilterAPIKeys{
	public static final String CLIENT_1_KEY = "X2jdj20xjvV";
	public static final String CLIENT_1_DN = "CN=Test Client 1,L=Test,O=Test";
	
	@Override
	public String getClientDN(String apiKey) {
		if( apiKey.equals(CLIENT_1_KEY) ){
			return CLIENT_1_DN;
		}
		return null;
	}

}
