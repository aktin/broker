package org.aktin.broker.auth.cred2.http;

import java.util.concurrent.TimeUnit;
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
import org.aktin.broker.auth.cred2.auth.Token;
import org.aktin.broker.auth.cred2.auth.TokenManager;
import org.aktin.broker.auth.cred2.service.UserAuthService;
import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.rest.RequireAdmin;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

@Path("auth/v2/")
public class AuthEndpoint {

  private static final Logger log = Logger.getLogger(AuthEndpoint.class.getName());

  @Inject
  private TokenManager manager;

  @Inject
  private UserAuthService auth;

  @POST
  @Path("login")
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.APPLICATION_XML)
  public String login(Credentials cred) {
    if (cred == null || cred.username == null || cred.username.isBlank() || cred.password == null || cred.password.isBlank()) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    String username = cred.username;
    char[] password = cred.password.toCharArray();
    boolean ok = auth.authenticate(username, password);
    if (!ok) {
      log.info(String.format("Access denied: %s", username));
      throw new ClientErrorException(Response.Status.UNAUTHORIZED);
    }
    Token t = manager.issue(username);
    log.info(String.format("Login successful: %s", username));
    return t.getGUID();
  }

  @GET
  @Authenticated
  @RequireAdmin
  @Path("status")
  @Produces(MediaType.APPLICATION_XML)
  public Status status(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer) {
    Token t = resolveTokenFromBearerHeader(bearer);
    Status s = new Status();
    s.username = t.getName();
    s.issued = t.issuedTimeMillis();
    s.expiresAt = t.expiresAtMillis();
    return s;
  }

  @POST
  @Authenticated
  @RequireAdmin
  @Path("logout")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.TEXT_PLAIN)
  public void logout(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer) {
    Token t = resolveTokenFromBearerHeader(bearer);
    long durationSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t.issuedTimeMillis());
    manager.revoke(t.getGUID());
    log.info(String.format("Logged out user: %s (Session duration: %d s)", t.getName(), durationSeconds));
  }

  private Token resolveTokenFromBearerHeader(String bearer) throws ClientErrorException {
    String guid = HttpBearerAuthentication.extractBearerToken(bearer);
    if (guid == null || guid.isBlank()) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    Token token = manager.lookup(guid);
    if (token == null) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    return token;
  }
}
