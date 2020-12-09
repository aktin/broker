package org.aktin.broker.auth;


import java.io.IOException;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;


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
public class AuthFilterSSLHeaders implements ContainerRequestFilter, HeaderAuthentication{
	private static final Logger log = Logger.getLogger(AuthFilterSSLHeaders.class.getName());
	/**
	 * Client ID to uniquely identify the client. 
	 * You can use the certificate serial number or the fingerprint value.
	 */
	public static final String X_SSL_CLIENT_ID = "X-SSL-Client-ID";
	public static final String X_SSL_CLIENT_DN = "X-SSL-Client-DN";
	public static final String X_SSL_CLIENT_VERIFY = "X-SSL-Client-Verify";

	@Inject
	private AuthCache authCache;
	
	@Override
	public void filter(ContainerRequestContext ctx) throws IOException {

		Principal principal = authenticateByHeaders(ctx::getHeaderString);
		if( principal == null ){
        	ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
		}else{
			ctx.setSecurityContext(principal);
		}
	}

	@Override
	public Principal authenticateByHeaders(Function<String, String> getHeader) {
		String verify = getHeader.apply(X_SSL_CLIENT_VERIFY);
		String id = getHeader.apply(X_SSL_CLIENT_ID);
		String dn = getHeader.apply(X_SSL_CLIENT_DN);
		if( verify == null || !verify.equals("SUCCESS") ){
        	log.info("Client verify header not found or not successful");
        	return null;
		}

		Principal principal;
		try {
			principal = authCache.getPrincipal(id, dn);
			log.info("Principal found: "+principal.getName());
			return principal;
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to lookup principal", e);
			return null;
		}		
	}
}
