package org.aktin.broker;

import java.io.IOException;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.aktin.broker.auth.AuthFilterAPIKeys;
import org.aktin.broker.auth.AuthFilterSSLHeaders;
import org.aktin.broker.auth.Principal;
import org.aktin.broker.rest.RequireAdmin;

@RequireAdmin
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthFilterAdmin implements ContainerRequestFilter{
	private static final Logger log = Logger.getLogger(AuthFilterAPIKeys.class.getName());

	@Override
	public void filter(ContainerRequestContext ctx) throws IOException {
		String verify = ctx.getHeaderString(AuthFilterSSLHeaders.X_SSL_CLIENT_VERIFY);
		String id = ctx.getHeaderString(AuthFilterSSLHeaders.X_SSL_CLIENT_ID);
		String dn = ctx.getHeaderString(AuthFilterSSLHeaders.X_SSL_CLIENT_DN);
		if( verify == null || !verify.equals("SUCCESS") ){
        	ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        	log.info("Client verify header not found or not successful");
        	return;
		}

		Principal principal = new Principal(Integer.parseInt(id), dn);
		
		if( !principal.isAdmin() ){
			log.info("no admin: "+principal);
			ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}
	}

}
