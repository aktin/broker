package org.aktin.broker.auth.cred2.service;

public interface UserAuthService {

  boolean authenticate(String username, char[] providedPassword);
}
