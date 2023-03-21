package org.aktin.broker.auth;

import java.io.IOException;
import java.util.Objects;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.HeaderAuthentication;

@Authenticated
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationRequestFilter implements ContainerRequestFilter{

	@Inject
	private AuthCache cache;
	@Inject
	private HeaderAuthentication auth;

	@Override
	public void filter(ContainerRequestContext ctx) throws IOException {
		Objects.requireNonNull(auth,"injection of HeaderAuthentication failed");
		Objects.requireNonNull(cache,"injection of AuthCache failed");
		
		AuthInfo info = auth.authenticateByHeaders(ctx::getHeaderString);
		if( info == null ) {
			ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
			return;
		}
		final Principal user = cache.getPrincipal(info);
		
		ctx.setSecurityContext(user);
	}

}
