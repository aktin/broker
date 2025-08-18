package org.aktin.broker.auth.cred2.auth;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

public class Token implements Principal {

  private static final String PROPERTY_TTL_SECONDS = "aktin.broker.token.lifespan";
  private static final long DEFAULT_TTL_SECONDS = 360L;

  private static final int ID_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final String user;
  private final long issued;
  private final String guid;

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
      throw new IllegalArgumentException("Token lifespan must be > 0");
    }
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
      long ttl = Long.getLong(PROPERTY_TTL_SECONDS, DEFAULT_TTL_SECONDS);
      long now = System.currentTimeMillis();
      this.lastAccess = now;
      this.expiresAt = now + ttl * 1000L;
    }
  }
}
