package org.aktin.broker.auth.otp.repository;

import java.util.Objects;
import java.util.Optional;

/**
 * An immutable data object representing a single user account. It serves as the data model for all persistent user attributes.
 */
public final class User {

  public final String username;
  public final String password;
  public final boolean active;
  public final long createdAt;
  public final Optional<String> token;

  public User(String username, String password, boolean active, long createdAt, Optional<String> token) {
    this.username = Objects.requireNonNull(username);
    this.password = Objects.requireNonNull(password);
    this.active = active;
    this.createdAt = createdAt;
    this.token = Objects.requireNonNull(token);
  }
}
