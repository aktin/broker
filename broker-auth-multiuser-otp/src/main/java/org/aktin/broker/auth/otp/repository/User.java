package org.aktin.broker.auth.otp.repository;

import java.util.Objects;

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
