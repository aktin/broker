package org.aktin.broker.auth.otp.controller;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.aktin.broker.auth.otp.token.Token;
import org.aktin.broker.auth.otp.token.TokenManager;
import org.aktin.broker.auth.otp.repository.User;
import org.aktin.broker.auth.otp.service.UserService;
import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.rest.RequireAdmin;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

// TODO add proper logging of user actions
@Path("users")
public class UserEndpoint {

  private static final Logger log = Logger.getLogger(UserEndpoint.class.getName());

  // must match JdbcUserService bootstrap property
  private static final String PROPERTY_ADMIN_USER = "aktin.broker.username";
  private static final String DEFAULT_ADMIN_USER = "admin";

  @Inject
  private TokenManager manager;

  @Inject
  private UserService service;

  @GET
  @Authenticated
  @RequireAdmin
  @Produces(MediaType.APPLICATION_XML)
  public List<UserDTO> list(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer) {
    requireDefaultAdmin(bearer);
    try {
      return service.list().stream().map(UserDTO::of).collect(Collectors.toList());
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error listing users", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Authenticated
  @RequireAdmin
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.APPLICATION_XML)
  public Response create(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, CredentialsDTO cred) {
    requireDefaultAdmin(bearer);
    if (cred == null || cred.username == null || cred.username.isBlank() || cred.password == null || cred.password.isBlank()) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    User user;
    try {
      user = service.get(cred.username);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error while getting user", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    }
    if (user != null) {
      throw new ClientErrorException(Response.Status.CONFLICT);
    }
    char[] pw = cred.password.toCharArray();
    try {
      service.create(cred.username, pw);
      return Response.status(Response.Status.CREATED).build();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error while creating new user", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Authenticated
  @RequireAdmin
  @Path("{username}/activate")
  public Response activate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, @PathParam("username") String username) {
    requireDefaultAdmin(bearer);
    if (username == null || username.isBlank()) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    try {
      service.activate(username);
      return Response.ok().build();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error while activating user", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Authenticated
  @RequireAdmin
  @Path("{username}/deactivate")
  public Response deactivate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, @PathParam("username") String username) {
    requireDefaultAdmin(bearer);
    if (username == null || username.isBlank()) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    String defaultUser = System.getProperty(PROPERTY_ADMIN_USER, DEFAULT_ADMIN_USER);
    if (username.equals(defaultUser)) {
      throw new ClientErrorException(Response.Status.FORBIDDEN);
    }
    try {
      service.deactivate(username);
      return Response.ok().build();
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error while deactivating user", e);
      throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private void requireDefaultAdmin(String bearer) {
    String guid = HttpBearerAuthentication.extractBearerToken(bearer);
    if (guid == null || guid.isBlank()) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    Token token = manager.lookup(guid);
    if (token == null) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    String defaultUser = System.getProperty(PROPERTY_ADMIN_USER, DEFAULT_ADMIN_USER);
    if (!Objects.equals(token.getName(), defaultUser)) {
      throw new ClientErrorException(Response.Status.FORBIDDEN);
    }
  }
}
