package org.aktin.broker.auth.otp.service;

public interface UserAuthService {

  boolean authenticate(String username, char[] providedPassword);
}
