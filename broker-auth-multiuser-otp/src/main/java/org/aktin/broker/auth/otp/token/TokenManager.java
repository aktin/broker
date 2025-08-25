package org.aktin.broker.auth.otp.token;

public interface TokenManager {

  Token issue(String username);

  Token lookup(String guid);

  void revoke(String guid);
}
