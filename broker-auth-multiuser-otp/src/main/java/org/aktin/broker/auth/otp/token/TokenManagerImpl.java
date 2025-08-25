package org.aktin.broker.auth.otp.token;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

@Singleton
public class TokenManagerImpl implements TokenManager {

  private final Map<String, Token> sessions = new ConcurrentHashMap<>();

  public TokenManagerImpl() {
  }

  @Override
  public Token issue(String username) {
    if (username == null) {
      return null;
    }
    Token token = new Token(username);
    sessions.put(token.getGUID(), token);
    return token;
  }

  @Override
  public Token lookup(String guid) {
    if (guid == null) {
      return null;
    }
    Token token = sessions.get(guid);
    if (token == null) {
      return null;
    }
    if (!token.isValid()) {
      sessions.remove(guid);
      return null;
    }
    return token;
  }

  @Override
  public void revoke(String guid) {
    Token token = sessions.remove(guid);
    if (token != null) {
      token.invalidate();
    }
  }
}
