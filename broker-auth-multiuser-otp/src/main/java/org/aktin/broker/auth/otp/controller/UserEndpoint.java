package org.aktin.broker.auth.otp.controller;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.aktin.broker.auth.otp.repository.OperationResult;
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

  /**
   * Lists all registered users. Requires default administrator privileges.
   * <ul>
   * <li>{@code 200} - Success. The body contains the list of users.</li>
   * <li>{@code 401} - The provided admin bearer token is invalid.</li>
   * </ul>
   *
   * @param bearer The administrator's session token.
   * @return A list of {@link UserDTO} objects.
   */
  @GET
  @Authenticated
  @RequireAdmin
  @Produces(MediaType.APPLICATION_XML)
  public List<UserDTO> list(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer) {
    EndpointUtils.requireDefaultAdmin(bearer, manager);
    return service.list().stream().map(UserDTO::of).collect(Collectors.toList());
  }

  /**
   * Creates a new user. Requires default administrator privileges.
   *
   * @param bearer The administrator's session token.
   * @param cred   A {@link CredentialsDTO} containing the new user's username and password.
   * @return A Response indicating the result of the operation: <ul>
   * <li>{@code 201} - User created successfully.</li>
   * <li>{@code 400} - Username or password not provided in the request body.</li>
   * <li>{@code 401} - The provided admin bearer token is invalid.</li>
   * <li>{@code 409} - A user with the specified username already exists.</li>
   * <li>{@code 500} - An internal server error occurred during user creation.</li>
   * </ul>
   */
  @POST
  @Authenticated
  @RequireAdmin
  @Produces(MediaType.TEXT_PLAIN)
  @Consumes(MediaType.APPLICATION_XML)
  public Response create(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, CredentialsDTO cred) {
    Token token = EndpointUtils.requireDefaultAdmin(bearer, manager);
    EndpointUtils.validateCredentials(cred);
    char[] pw = cred.password.toCharArray();
    OperationResult result = service.create(cred.username, pw);
    switch (result) {
      case SUCCESS:
        log.info(String.format("User %s created new user %s", token.getName(), cred.username));
        return Response.status(Status.CREATED).build();
      case USER_ALREADY_EXISTS:
        log.warning(String.format("User %s failed to create user %s: already exists", token.getName(), cred.username));
        throw new ClientErrorException(Status.CONFLICT);
      default:
        log.warning(String.format("User %s failed to create user %s", token.getName(), cred.username));
        throw new ClientErrorException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Activates a user account, allowing them to log in. Requires default administrator privileges.
   *
   * @param bearer   The administrator's session token.
   * @param username The username of the account to activate.
   * @return A Response indicating the result of the operation: <ul>
   * <li>{@code 200} - User activated successfully.</li>
   * <li>{@code 401} - The provided admin bearer token is invalid.</li>
   * <li>{@code 404} - The specified user was not found.</li>
   * <li>{@code 500} - An internal server error occurred.</li>
   * </ul>
   */
  @POST
  @Authenticated
  @RequireAdmin
  @Path("{username}/active")
  public Response activate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, @PathParam("username") String username) {
    Token token = EndpointUtils.requireDefaultAdmin(bearer, manager);
    OperationResult result = service.activate(username);
    switch (result) {
      case SUCCESS:
        log.info(String.format("User %s activated user %s", token.getName(), username));
        return Response.status(Status.OK).build();
      case USER_NOT_FOUND:
        log.warning(String.format("User %s failed to activate user %s: not found", token.getName(), username));
        throw new ClientErrorException(Status.NOT_FOUND);
      default:
        log.warning(String.format("User %s failed to activate user %s", token.getName(), username));
        throw new ClientErrorException(Status.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Deactivates a user account, preventing them from logging in. Requires default administrator privileges. The default administrator user cannot be deactivated.
   *
   * @param bearer   The administrator's session token.
   * @param username The username of the account to deactivate.
   * @return A Response indicating the result of the operation: <ul>
   * <li>{@code 200} - User deactivated successfully.</li>
   * <li>{@code 401} - The provided admin bearer token is invalid.</li>
   * <li>{@code 403} - Deactivating the default administrator is forbidden.</li>
   * <li>{@code 404} - The specified user was not found.</li>
   * <li>{@code 500} - An internal server error occurred.</li>
   * </ul>
   */
  @DELETE
  @Authenticated
  @RequireAdmin
  @Path("{username}/active")
  public Response deactivate(@HeaderParam(HttpHeaders.AUTHORIZATION) String bearer, @PathParam("username") String username) {
    Token token = EndpointUtils.requireDefaultAdmin(bearer, manager);
    if (EndpointUtils.checkForDefaultUser(username)) {
      log.warning(String.format("User %s tried to deactivate default user %s", token.getName(), username));
      throw new ClientErrorException(Response.Status.FORBIDDEN);
    }
    OperationResult result = service.deactivate(username);
    switch (result) {
      case SUCCESS:
        log.info(String.format("User %s deactivated user %s", token.getName(), username));
        return Response.status(Status.OK).build();
      case USER_NOT_FOUND:
        log.warning(String.format("User %s failed to deactivate user %s: not found", token.getName(), username));
        throw new ClientErrorException(Status.NOT_FOUND);
      default:
        log.warning(String.format("User %s failed to deactivate user %s", token.getName(), username));
        throw new ClientErrorException(Status.INTERNAL_SERVER_ERROR);
    }
  }
}
