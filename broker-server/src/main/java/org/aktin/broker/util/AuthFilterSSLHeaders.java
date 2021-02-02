package org.aktin.broker.util;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.AuthRole;
import org.aktin.broker.server.auth.HeaderAuthentication;


/**
 * Authentication filter for RESTful interfaces.
 * <p>
 * After authentication, security is handled by the SecurityContext
 * interface. Since the security context has no {@code isAdmin} method,
 * a special role "admin" is used for that purpose.
 * </p>
 * 
 * Extend this class for TLS client certificate authentication 
 *<pre>
 *  {@literal @}Authenticated
 *  {@literal @}Provider
 *  {@literal @}Priority(Priorities.AUTHENTICATION)
 *</pre>
 *
 * @author R.W.Majeed
 *
 */
//@Authenticated
//@Provider
//@Priority(Priorities.AUTHENTICATION)
public class AuthFilterSSLHeaders implements HeaderAuthentication{
	private static final Logger log = Logger.getLogger(AuthFilterSSLHeaders.class.getName());
	/**
	 * Client ID to uniquely identify the client. 
	 * You can use the certificate serial number or the fingerprint value.
	 */
	public static final String X_SSL_CLIENT_ID = "X-SSL-Client-ID";
	public static final String X_SSL_CLIENT_DN = "X-SSL-Client-DN";
	public static final String X_SSL_CLIENT_VERIFY = "X-SSL-Client-Verify";

	/**
	 * Override this method to derive the user roles from client DN.
	 * Default implementation looks if the DN contains the string {@code OU=admin}
	 * to differentiate between admin and node roles.
	 *
	 * @param clientDN client dn
	 * @return set of roles
	 */
	public Set<AuthRole> loadRolesFromClientDN(String clientDN){
		if( clientDN.contains("OU=admin") ) {
			return new HashSet<>(Arrays.asList(AuthRole.ADMIN_READ, AuthRole.ADMIN_WRITE));
		}else {
			return new HashSet<>(Arrays.asList(AuthRole.NODE_READ, AuthRole.NODE_WRITE));
		}
	}
	@Override
	public AuthInfo authenticateByHeaders(Function<String, String> getHeader) throws IOException {
		String verify = getHeader.apply(X_SSL_CLIENT_VERIFY);
		String id = getHeader.apply(X_SSL_CLIENT_ID);
		String dn = getHeader.apply(X_SSL_CLIENT_DN);
		if( verify == null || !verify.equals("SUCCESS") ){
			// authentication failed
        	log.info("Client verify header not found or not successful");
        	return null;
		}else {
			log.info("Authenticated user "+id+" with dn "+dn);
		}
		return new AuthInfoImpl(id, dn, loadRolesFromClientDN(dn));
	}

}
