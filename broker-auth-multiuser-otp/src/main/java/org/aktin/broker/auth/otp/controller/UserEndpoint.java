package org.aktin.broker.auth.otp.controller;

import java.util.List;
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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.aktin.broker.auth.otp.repository.User;
import org.aktin.broker.auth.otp.service.UserService;
import org.aktin.broker.auth.otp.token.Token;
import org.aktin.broker.auth.otp.token.TokenManager;
import org.aktin.broker.auth.otp.utils.EndpointUtils;
import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.rest.RequireAdmin;

@Path("users")
public class UserEndpoint {

  private static final Logger log = Logger.getLogger(UserEndpoint.class.getName());

  @Inject
  private TokenManager manager;

  @Inject
  private UserService service;

  @GET
  @Authenticated
  @RequireAdmin
  @Produces(MediaType.APPLICATION_XML)
  public List<UserDTO> list(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer) {
    EndpointUtils.requireDefaultAdmin(bearer, manager);
    return service.list().stream().map(UserDTO::of).collect(Collectors.toList());
  }

  @POST
  @Authenticated
  @RequireAdmin
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.APPLICATION_XML)
  public Response create(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, CredentialsDTO cred) {
    Token token = EndpointUtils.requireDefaultAdmin(bearer, manager);
    log.info(String.format("User %s attempts to create new user: %s", token.getName(), cred.username));
    EndpointUtils.validateCredentials(cred);
    User user = service.get(cred.username);
    if (user != null) {
      log.warning("User already exists: " + cred.username);
      throw new ClientErrorException(Response.Status.CONFLICT);
    }
    char[] pw = cred.password.toCharArray();
    boolean success = service.create(cred.username, pw);
    if (success) {
      return Response.status(Response.Status.CREATED).build();
    }
    throw new ClientErrorException(Response.Status.INTERNAL_SERVER_ERROR);
  }

  @POST
  @Authenticated
  @RequireAdmin
  @Path("{username}/activate")
  public Response activate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, @PathParam("username") String username) {
    Token token = EndpointUtils.requireDefaultAdmin(bearer, manager);
    log.info(String.format("User %s attempts to activate user: %s", token.getName(), username));
    boolean success = service.activate(username);
    if (success) {
      return Response.ok().build();
    }
    throw new ClientErrorException(Response.Status.NOT_FOUND);
  }

  @POST
  @Authenticated
  @RequireAdmin
  @Path("{username}/deactivate")
  public Response deactivate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, @PathParam("username") String username) {
    Token token = EndpointUtils.requireDefaultAdmin(bearer, manager);
    log.info(String.format("User %s attempts to deactivate user: %s", token.getName(), username));
    if (EndpointUtils.checkForDefaultUser(username)) {
      log.warning("Cannot deactivate default admin user: " + username);
      throw new ClientErrorException(Response.Status.FORBIDDEN);
    }
    boolean success = service.deactivate(username);
    if (success) {
      return Response.ok().build();
    }
    throw new ClientErrorException(Response.Status.NOT_FOUND);
  }
}
