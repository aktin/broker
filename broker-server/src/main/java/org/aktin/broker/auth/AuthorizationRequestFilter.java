package org.aktin.broker.auth;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.aktin.broker.rest.RequireAdmin;

/**
 * Authorization request filter. At the moment, only admin authorization is verified.
 * For more detailed authorization, this class should be updated to allow annotation arguments
 * e.g. like {@code RequireRole(ADMIN_READ)}
 * 
 * @author R.W.Majeed
 *
 */
@RequireAdmin
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthorizationRequestFilter implements ContainerRequestFilter{
	private static final Logger log = Logger.getLogger(AuthorizationRequestFilter.class.getName());

	@Override
	public void filter(ContainerRequestContext ctx) throws IOException {
		Principal user = (Principal)ctx.getSecurityContext().getUserPrincipal();
		// make sure user is authenticated and has admin privileges
		if( user == null || !user.isAdmin() ) {
			log.info("Authorization admin denied to "+user);
			ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;			
		}
	}

}
