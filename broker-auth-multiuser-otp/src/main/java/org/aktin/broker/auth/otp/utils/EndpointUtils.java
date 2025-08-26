package org.aktin.broker.auth.otp.utils;

import java.util.Objects;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.aktin.broker.auth.otp.controller.CredentialsDTO;
import org.aktin.broker.auth.otp.token.Token;
import org.aktin.broker.auth.otp.token.TokenManager;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

public class EndpointUtils {

  // must match UserService implementation bootstrap property
  private static final String PROPERTY_ADMIN_USER = "aktin.broker.username";
  private static final String DEFAULT_ADMIN_USER = "admin";

  public static void validateCredentials(CredentialsDTO cred) {
    if (cred == null || cred.username == null || cred.username.isBlank() || cred.password == null || cred.password.isBlank()) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
  }

  public static boolean checkForDefaultUser(String username) {
    String defaultUser = System.getProperty(PROPERTY_ADMIN_USER, DEFAULT_ADMIN_USER);
    return username.equals(defaultUser);
  }

  public static Token resolveTokenFromBearerHeader(String bearer, TokenManager manager) {
    String guid = HttpBearerAuthentication.extractBearerToken(bearer);
    if (guid == null || guid.isBlank()) {
      throw new ClientErrorException(Response.Status.BAD_REQUEST);
    }
    Token token = manager.lookup(guid);
    if (token == null) {
      throw new ClientErrorException(Response.Status.UNAUTHORIZED);
    }
    return token;
  }

  public static Token requireDefaultAdmin(String bearer, TokenManager manager) {
    Token token = resolveTokenFromBearerHeader(bearer, manager);
    String defaultUser = System.getProperty(PROPERTY_ADMIN_USER, DEFAULT_ADMIN_USER);
    if (!Objects.equals(token.getName(), defaultUser)) {
      throw new ClientErrorException(Status.UNAUTHORIZED);
    }
    return token;
  }
}
