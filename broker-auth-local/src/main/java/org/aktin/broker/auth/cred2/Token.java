package org.aktin.broker.auth.cred2;

import java.security.Principal;

public class Token implements Principal {

  private final Integer userId;
  private final String user;
  private final long issued;

  public Token(String user, int userId) {
    this.userId = userId;
    this.user = user;
    this.issued = System.currentTimeMillis();
  }

  public String getGUID() {
    return Long.toHexString(System.identityHashCode(this) * this.issued);
  }

  public long issuedTimeMillis() {
    return issued;
  }

  @Override
  public String getName() {
    return user;
  }

  public boolean isAdmin() {
    return false;
  }

  public void renew() {
    // TODO
  }

  public void invalidate() {
    // TODO
  }
}
