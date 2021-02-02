package org.aktin.broker.auth;

import java.io.IOException;
import java.util.function.Function;

import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.HeaderAuthentication;

public class CascadedHeaderAuthentication implements HeaderAuthentication {
	private HeaderAuthentication[] delegates;

	public CascadedHeaderAuthentication(HeaderAuthentication[] delegates) {
		this.delegates = delegates;
	}
	@Override
	public AuthInfo authenticateByHeaders(Function<String, String> getHeader) throws IOException {
		AuthInfo info = null;
		for( int i=0; i<delegates.length; i++ ) {
			info = delegates[i].authenticateByHeaders(getHeader);
			if( info != null ) {
				break;
			}
		}
		return info;
	}

	
}
