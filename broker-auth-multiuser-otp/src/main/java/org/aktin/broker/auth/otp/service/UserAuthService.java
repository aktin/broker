package org.aktin.broker.auth.otp.service;

/**
 * Defines the contract for the primary user authentication service.
 */
public interface UserAuthService {

  boolean authenticate(String username, char[] providedPassword, String token);
}
