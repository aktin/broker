package org.aktin.broker.auth.cred2.repository;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Immutable view of a row in the "users" table (see userCreds.xml).
 */
public final class User {

  public final String username;
  public final String password;
  public final String algorithm;
  public final boolean active;
  public final long createdAt;

  public User(String username, String password, String algorithm, boolean active, long createdAt) {
    this.username = Objects.requireNonNull(username);
    this.password = Objects.requireNonNull(password);
    this.algorithm = Objects.requireNonNull(algorithm);
    this.active = active;
    this.createdAt = createdAt;
  }
}
