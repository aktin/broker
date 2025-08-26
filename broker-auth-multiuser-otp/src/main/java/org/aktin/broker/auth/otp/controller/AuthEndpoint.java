package org.aktin.broker.auth.otp.controller;

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
import org.aktin.broker.auth.otp.service.UserAuthService;
import org.aktin.broker.auth.otp.token.Token;
import org.aktin.broker.auth.otp.token.TokenManager;
import org.aktin.broker.auth.otp.utils.EndpointUtils;
import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.rest.RequireAdmin;

@Path("auth")
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
  public String login(CredentialsDTO cred) {
    EndpointUtils.validateCredentials(cred);
    String username = cred.username;
    char[] password = cred.password.toCharArray();
    String token = cred.token;
    boolean ok = auth.authenticate(username, password, token);
    if (!ok) {
      throw new ClientErrorException(Response.Status.UNAUTHORIZED);
    }
    Token t = manager.issue(username);
    return t.getGUID();
  }

  @GET
  @Authenticated
  @RequireAdmin
  @Path("status")
  @Produces(MediaType.APPLICATION_XML)
  public StatusDTO status(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer) {
    Token t = EndpointUtils.resolveTokenFromBearerHeader(bearer, manager);
    StatusDTO s = new StatusDTO();
    s.username = t.getName();
    s.issued = t.issuedTimeMillis();
    s.expiresAt = t.expiresAtMillis();
    return s;
  }

  @POST
  @Authenticated
  @RequireAdmin
  @Path("logout")
  public void logout(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer) {
    Token t = EndpointUtils.resolveTokenFromBearerHeader(bearer, manager);
    long durationSeconds = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - t.issuedTimeMillis());
    manager.revoke(t.getGUID());
    log.info(String.format("Logged out user: %s (Session duration: %d s)", t.getName(), durationSeconds));
  }
}
