package org.aktin.broker.auth.otp.token;

import java.util.logging.Logger;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

/**
 * Integrates the custom {@link TokenManager} with the AKTIN Broker's authentication system.
 */
public class CredentialTokenAuth extends HttpBearerAuthentication {

  private static final Logger log = Logger.getLogger(CredentialTokenAuth.class.getName());

  private final TokenManager manager;

  public CredentialTokenAuth(TokenManager manager) {
    this.manager = manager;
  }

  //TODO all users default to admin role
  private static String getClientDn(Token token) {
    return "CN=" + token.getName() + ",OU=admin";
  }

  /**
   * Looks up authentication information for a given token GUID.
   * <p>
   * This method is called by the AKTIN Broker framework for each authenticated request. It finds the corresponding {@link Token}, renews its lifespan, and constructs an {@link AuthInfo} object
   * containing the user's identity.
   *
   * @param guid The bearer token string extracted from the HTTP Authorization header.
   * @return An {@link AuthInfo} object if the token is valid, or {@code null} otherwise.
   */
  @Override
  protected AuthInfo lookupAuthInfo(String guid) {
    if (guid == null || guid.isEmpty()) {
      return null;
    }
    Token token = manager.lookup(guid);
    if (token == null) {
      return null; // invalid/expired/revoked
    }
    try {
      token.renew();
    } catch (RuntimeException e) {
      log.warning(String.format("Failed to renew token for user %s: %s", token.getName(), e.getMessage()));
    }
    final String dn = getClientDn(token);
    return new AuthInfoImpl(token.getName(), dn, defaultRolesForClientDN(dn));
  }
}
