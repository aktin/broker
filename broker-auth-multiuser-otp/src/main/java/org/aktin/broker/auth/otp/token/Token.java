package org.aktin.broker.auth.otp.token;

import java.security.Principal;
import java.util.Objects;

/**
 * Represents a time-limited session token that authenticates a user.
 */
public class Token implements Principal {

  private final String user;
  private final String guid;
  private final long issued;
  private final long ttl;

  // mutable values
  // lastAccess and expiresAt in ms
  private volatile boolean revoked;
  private volatile long lastAccess;
  private volatile long expiresAt;

  public Token(String user, String guid, long tokenTimeToLive) {
    this.user = Objects.requireNonNull(user);
    this.guid = Objects.requireNonNull(guid);
    this.ttl = tokenTimeToLive;
    this.issued = System.currentTimeMillis();
    this.lastAccess = this.issued;
    this.expiresAt = this.issued + tokenTimeToLive * 1000L;
    this.revoked = false;
  }

  @Override
  public String getName() {
    return user;
  }

  public String getGUID() {
    return guid;
  }

  public boolean isAdmin() {
    return false;
  }

  public long issuedTimeMillis() {
    return issued;
  }

  public long lastAccessMillis() {
    return lastAccess;
  }

  public long expiresAtMillis() {
    return expiresAt;
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

  //new expiration is set to the current time plus the token's original TTL.
  public synchronized void renew() {
    if (isValid()) {
      long now = System.currentTimeMillis();
      this.lastAccess = now;
      this.expiresAt = now + this.ttl * 1000L;
    }
  }
}
