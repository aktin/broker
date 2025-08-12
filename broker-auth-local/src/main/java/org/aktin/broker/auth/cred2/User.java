package org.aktin.broker.auth.cred2;

public class User {

  public final String username;
  public final String password;
  public final boolean active;

  public User(String username, String password, boolean active) {
    this.username = username;
    this.password = password;
    this.active = active;
  }
}