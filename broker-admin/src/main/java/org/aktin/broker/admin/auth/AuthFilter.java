package org.aktin.broker.admin.auth;

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

import org.aktin.broker.RequireAdmin;


/**
 * Authentication filter for RESTful interfaces.
 * <p>
 * After authentication, security is handled by the SecurityContext
 * interface. Since the security context has no {@code isAdmin} method,
 * a special role "admin" is used for that purpose.
 * </p>
 * 
 * @author R.W.Majeed
 *
 */
@RequireAdmin
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter{
	private static final Logger log = Logger.getLogger(AuthFilter.class.getName());
	
	@Inject
	private TokenManager tokens;
	
	@Override
	public void filter(ContainerRequestContext ctx) throws IOException {
		String authHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
		
		// Check if the HTTP Authorization header is present and formatted correctly 
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new NotAuthorizedException("Authorization header must be provided");
		}
		
		// Extract the token from the HTTP Authorization header
		String token = authHeader.substring("Bearer".length()).trim();

		try{
			log.info("Authentication token: "+token);
			Token t = validateToken(token);
			// renew token with every access
			t.renew();
			ctx.setSecurityContext(getSecurityContext(t));
			
        }catch( Exception e ){
        	ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
	}
	
	private Token validateToken(String token) throws IOException{
		Token t = tokens.lookupToken(token);
		if( t == null ){
			log.info("Token not found: "+token);//+" (total "+tokens.getTokenCount()+")");
			throw new IOException("Access denied");
		}else{
			log.info("Token found, user="+t.getName());
		}
		return t;
	}
	private SecurityContext getSecurityContext(Token token){
		// TODO serious implementation
		return new SecurityContext() {
			
			@Override
			public boolean isUserInRole(String role) {
				if( role.equals("admin") ){
					return token.isAdmin();
				}else{
					return token.hasRole(role);
				}
			}
			
			@Override
			public boolean isSecure() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public Principal getUserPrincipal() {
				return token;
			}
			
			@Override
			public String getAuthenticationScheme() {
				return null;
			}
		};		
	}

}
