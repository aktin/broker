package org.aktin.broker.auth.cred2.auth;

public interface TokenManager {

  Token issue(String username);

  Token lookup(String guid);

  void revoke(String guid);

  void pruneInvalid();
}
