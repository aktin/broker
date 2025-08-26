package org.aktin.broker.auth.otp.token;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.logging.Logger;

public class Token implements Principal {

  private static final Logger log = Logger.getLogger(Token.class.getName());

  private static final String PROPERTY_TTL_SECONDS = "aktin.broker.auth.token.lifespan";
  private static final long DEFAULT_TTL_SECONDS = 360L;

  private static final int ID_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final String user;
  private final long issued;
  private final String guid;
  private final long ttl;

  // Mutable Values, lastAccess and expiresAt in Milliseconds
  private volatile boolean revoked;
  private volatile long lastAccess;
  private volatile long expiresAt;

  public Token(String user) {
    this(user, Long.getLong(PROPERTY_TTL_SECONDS, DEFAULT_TTL_SECONDS));
  }

  public Token(String user, long tokenTimeToLive) {
    this.user = Objects.requireNonNull(user);
    if (tokenTimeToLive <= 0) {
      log.warning("Token lifespan must be > 0. Using default lifespan.");
      tokenTimeToLive = DEFAULT_TTL_SECONDS;
    }
    this.ttl = tokenTimeToLive;
    this.issued = System.currentTimeMillis();
    this.guid = generateGUID();
    this.lastAccess = this.issued;
    this.expiresAt = this.issued + tokenTimeToLive * 1000L;
    this.revoked = false;
  }

  private static String generateGUID() {
    byte[] buf = new byte[ID_BYTES];
    RANDOM.nextBytes(buf);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
  }

  @Override
  public String getName() {
    return user;
  }

  public boolean isAdmin() {
    return false;
  }

  public long issuedTimeMillis() {
    return issued;
  }

  public String getGUID() {
    return guid;
  }

  public long expiresAtMillis() {
    return expiresAt;
  }

  public long lastAccessMillis() {
    return lastAccess;
  }

  public boolean isExpired() {
    return expiresAt <= System.currentTimeMillis();
  }

  public boolean isValid() {
    return !revoked && !isExpired();
  }

  public synchronized void invalidate() {
    this.revoked = true;
  }

  public synchronized void renew() {
    if (isValid()) {
      long now = System.currentTimeMillis();
      this.lastAccess = now;
      this.expiresAt = now + this.ttl * 1000L;
    }
  }
}
