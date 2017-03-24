package org.aktin.broker.admin.auth;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;


/**
 * RESTful authentication endpoint. Log on/off users
 * via application/json calls
 * 
 * Example usage:
 * <pre>
 curl -H "Content-Type: application/json" -X POST -d '{"username":"admin","password":"xyz"}' http://localhost:8080/aktin/admin/auth/login
 
 </pre>
 * Send token header:
 * <pre>
 * curl -H "Authorization: Bearer fe4798-1d90-41d4-a228-21e891d2bb65" http://localhost:8080/aktin/admin/auth/test
 * 

 * </pre>
 * @author R.W.Majeed
 *
 */
@Path("auth")
public class AuthEndpoint {
	private static final Logger log = Logger.getLogger(AuthEndpoint.class.getName());
	@Inject 
	private TokenManager tokens;
	
	@Context 
	private SecurityContext security;
	
	@POST
	@Path("login")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response authenticateUser(Credentials cred){
		// TODO allow access for other users
		Token t = tokens.authenticate(cred.username, cred.password.toCharArray());
		if( t != null ){
			log.info("Login successful: "+cred.username);
			return Response.ok(t.getGUID()).build();
		}else{
			log.info("Access denied for "+cred.username);
			// access denied
			return Response.status(Response.Status.UNAUTHORIZED).build();
		}
	}
	
	@POST
	@Secured
	@Path("logout")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.TEXT_PLAIN)
	public String logout(String token){
		Token t = tokens.lookupToken(token);
		t.invalidate();
		return "{duration="+(System.currentTimeMillis()-t.issuedTimeMillis())+"}";
	}

}
