package org.aktin.broker.auth;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.aktin.broker.RequireAdmin;

@RequireAdmin
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthFilterAdmin implements ContainerRequestFilter{
	private static final Logger log = Logger.getLogger(AuthFilterAPIKeys.class.getName());

	@Override
	public void filter(ContainerRequestContext ctx) throws IOException {
		SecurityContext sc = ctx.getSecurityContext();
		if( sc == null ){
			log.info("no security context");
			ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}
		java.security.Principal p = sc.getUserPrincipal();
		if( p == null || !(p instanceof Principal) ){
			log.info("no principal or wrong instance: "+p);
			ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}
		if( !((Principal)p).isAdmin() ){
			log.info("no admin: "+p);
			ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}
	}

}
