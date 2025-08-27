package org.aktin.broker.auth.otp.token;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.inject.Singleton;

@Singleton
public class TokenManagerImpl implements TokenManager {

  private static final Logger log = Logger.getLogger(TokenManagerImpl.class.getName());

  private static final String PROPERTY_TTL_SECONDS = "aktin.broker.auth.token.lifespan";
  private static final long DEFAULT_TTL_SECONDS = 300L;

  private static final int ID_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final long ttl;
  private final Map<String, Token> sessions = new ConcurrentHashMap<>();

  public TokenManagerImpl() {
    this(Long.getLong(PROPERTY_TTL_SECONDS, DEFAULT_TTL_SECONDS));
  }

  public TokenManagerImpl(long tokenTimeToLive) {
    if (tokenTimeToLive <= 0) {
      log.warning("Token lifespan must be > 0. Using default lifespan.");
      tokenTimeToLive = DEFAULT_TTL_SECONDS;
    }
    this.ttl = tokenTimeToLive;
  }

  @Override
  public Token issue(String username) {
    if (username == null) {
      return null;
    }
    String guid = generateGUID();
    Token token = new Token(username, guid, ttl);
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

  private String generateGUID() {
    byte[] buf = new byte[ID_BYTES];
    RANDOM.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }
}
