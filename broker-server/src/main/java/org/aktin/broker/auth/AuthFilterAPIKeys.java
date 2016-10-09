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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.aktin.broker.Authenticated;

/**
 * Extend this class for API key authentication.
 * 
 *  <pre>
 *  @Authenticated
@Provider
@Priority(Priorities.AUTHENTICATION)

 * @author R.W.Majeed
 *
 */
public abstract class AuthFilterAPIKeys implements ContainerRequestFilter {
	private static final Logger log = Logger.getLogger(AuthFilterAPIKeys.class.getName());

	@Inject
	private AuthCache authCache;

	/**
	 * Get the client directory name for the specified API key.
	 * 
	 * @param apiKey API key string as specified by the client
	 * @return client DN string or {@code null} to deny access.
	 */
	public abstract String getClientDN(String apiKey);

	@Override
	public final void filter(ContainerRequestContext ctx) throws IOException {
		
		String auth = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
		String key = null;
		if( auth != null && auth.startsWith("Bearer ") ){
			key = auth.substring(7);
		}
		if( key == null ){
        	ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        	log.info("HTTP Authorization header missing");
        	return;
		}

		// TODO check API key against whitelist

		Principal principal;
		try {
			principal = authCache.getPrincipal(key, getClientDN(key));
			ctx.setSecurityContext(principal);
			log.info("Principal found: "+principal.getName());
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Unable to lookup principal", e);
			ctx.abortWith(Response.serverError().build());
		}		
	}

}
