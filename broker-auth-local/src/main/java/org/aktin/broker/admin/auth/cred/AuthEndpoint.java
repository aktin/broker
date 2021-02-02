package org.aktin.broker.admin.auth.cred;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.rest.RequireAdmin;
import org.aktin.broker.server.auth.HttpBearerAuthentication;


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

	@POST
	@Path("login")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_XML)
	public String authenticateUser(Credentials cred){
		// TODO allow access for other users
		Token t = tokens.authenticate(cred.username, cred.password.toCharArray());
		if( t != null ){
			log.log(Level.INFO,"Login successful: {0}",cred.username);
			return t.getGUID();
		}else{
			log.log(Level.INFO,"Access denied for {0}",cred.username);
			// access denied
			throw new ClientErrorException(Response.Status.UNAUTHORIZED);
		}
	}

	private Token resolveTokenFromBearerHeader(String bearer) throws ClientErrorException {
		String key = HttpBearerAuthentication.extractBearerToken(bearer);
		if( key == null ) {
			throw new ClientErrorException(Response.Status.BAD_REQUEST);
		}
		Token token = tokens.lookupToken(key);
		if( token == null ) {
			throw new ClientErrorException(Response.Status.BAD_REQUEST);
		}
		return token;
	}
	@GET
	@Authenticated
	@RequireAdmin
	@Path("status")
	@Produces(MediaType.APPLICATION_XML)
	public Status getStatus(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer){
		Token t = resolveTokenFromBearerHeader(bearer);
		Status s = new Status();
		s.issued = t.issuedTimeMillis();
		return s;
	}
	@POST
	@Authenticated
	@RequireAdmin
	@Path("logout")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.TEXT_PLAIN)
	public String logout(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer){
		Token t = resolveTokenFromBearerHeader(bearer);
		t.invalidate();
		return "{duration="+(System.currentTimeMillis()-t.issuedTimeMillis())+"}";
	}

}
