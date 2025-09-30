package org.aktin.broker.auth.otp.repository;

/**
 * Represents the outcome of a {@link UserRepository} operation (a clearer status than just a simple boolean).
 */
public enum OperationResult {
  SUCCESS,
  USER_ALREADY_EXISTS,
  USER_NOT_FOUND,
  FAILED
}
