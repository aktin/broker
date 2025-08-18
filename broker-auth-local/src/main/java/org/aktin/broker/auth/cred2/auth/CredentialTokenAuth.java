package org.aktin.broker.auth.cred2.auth;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

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
      log.log(Level.WARNING, "Failed to renew token", e);
    }
    final String dn = getClientDn(token);
    return new AuthInfoImpl(token.getName(), dn, defaultRolesForClientDN(dn));
  }
}
