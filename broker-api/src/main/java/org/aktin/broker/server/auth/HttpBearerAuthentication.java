package org.aktin.broker.server.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Authentication with HTTP Authorization header with Bearer token.
 *
 * @author R.W.Majeed
 *
 */
public abstract class HttpBearerAuthentication implements HeaderAuthentication {
	private static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
	private static final Logger log = Logger.getLogger(HttpBearerAuthentication.class.getName());

	/**
	 * Get the auth info for the specified token.
	 * 
	 * @param token token/API key string as specified by the client
	 * @return auth info or {@code null} to deny access.
	 */
	protected abstract AuthInfo lookupAuthInfo(String token) throws IOException;

	/**
	 * Retrieve token from authorization header. Verifies that the header value starts with {@code Bearer}
	 * and then removes the prefix.
	 * @param authorizationHeader authorization header
	 * @return token or {@code null} if the header value is not prefixed with {@code Bearer}
	 */
	public static String extractBearerToken(String authorizationHeader) {
		String key = null;
		if( authorizationHeader != null && authorizationHeader.startsWith("Bearer ") ){
			key = authorizationHeader.substring(7);
		}
		return key;
	}

	public static Set<AuthRole> defaultRolesForClientDN(String clientDn){
		if( clientDn == null ) {
			return null;
		}
		if( clientDn.contains("OU=admin") ) {
			return new HashSet<>(Arrays.asList(AuthRole.ADMIN_READ,AuthRole.ADMIN_WRITE));
		}else {
			return new HashSet<>(Arrays.asList(AuthRole.NODE_READ,AuthRole.NODE_WRITE));
		}
	}

	@Override
	public AuthInfo authenticateByHeaders(Function<String,String> getHeader) throws IOException {
		String key = extractBearerToken(getHeader.apply(HTTP_HEADER_AUTHORIZATION));
		if( key == null ){
        	log.info("HTTP Authorization header missing");
        	return null;
		}

		return lookupAuthInfo(key);
	}

}
