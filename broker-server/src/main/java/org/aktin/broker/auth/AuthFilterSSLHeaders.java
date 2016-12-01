package org.aktin.broker.auth;


import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.aktin.broker.Authenticated;
import org.aktin.broker.auth.Principal;

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
public class AuthFilterSSLHeaders implements ContainerRequestFilter{
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
		String verify = ctx.getHeaderString(X_SSL_CLIENT_VERIFY);
		String id = ctx.getHeaderString(X_SSL_CLIENT_ID);
		String dn = ctx.getHeaderString(X_SSL_CLIENT_DN);
		if( verify == null || !verify.equals("SUCCESS") ){
        	ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        	log.info("Client verify header not found or not successful");
        	return;
		}

		Principal principal;
		try {
			principal = authCache.getPrincipal(id, dn);
			ctx.setSecurityContext(principal);
			log.info("Principal found: "+principal.getName());
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to lookup principal", e);
			ctx.abortWith(Response.serverError().build());
		}		
	}
}
