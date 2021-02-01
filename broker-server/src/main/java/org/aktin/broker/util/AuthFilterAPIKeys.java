package org.aktin.broker.util;

import java.io.IOException;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;

import org.aktin.broker.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthRole;
import org.aktin.broker.server.auth.HeaderAuthentication;

/**
 * Extend this class for API key authentication.
 * 
 *<pre>
 *  {@literal @}Authenticated
 *  {@literal @}Provider
 *  {@literal @}Priority(Priorities.AUTHENTICATION)
 *</pre>
 * @author R.W.Majeed
 *
 */
public abstract class AuthFilterAPIKeys implements HeaderAuthentication {
	private static final Logger log = Logger.getLogger(AuthFilterAPIKeys.class.getName());


	/**
	 * Get the client directory name for the specified API key.
	 * 
	 * @param apiKey API key string as specified by the client
	 * @return client DN string or {@code null} to deny access.
	 */
	public abstract String getClientDN(String apiKey);
	public abstract Set<AuthRole> getRoles(String apiKey);

	private AuthInfo processAuthorizationBearer(String bearer) throws IOException {
		String key = null;
		if( bearer != null && bearer.startsWith("Bearer ") ){
			key = bearer.substring(7);
		}
		if( key == null ){
        	log.info("HTTP Authorization header missing");
        	return null;
		}

		// check API key against whitelist
		String clientDn = getClientDN(key);
		if( clientDn == null ){
			// access denied
			log.info("Access denied for API key: "+key);
			return null;
		}
		// we found the clientDn -> client successfully authenticated

		return new AuthInfoImpl(key, clientDn, getRoles(key)); 
	}
	@Override
	public AuthInfo authenticateByHeaders(Function<String,String> getHeader) throws IOException {
		String auth = getHeader.apply(HttpHeaders.AUTHORIZATION);
		return processAuthorizationBearer(auth);
	}

}
