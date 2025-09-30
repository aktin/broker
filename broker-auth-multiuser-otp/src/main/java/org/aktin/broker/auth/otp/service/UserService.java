package org.aktin.broker.auth.otp.service;

import java.util.List;
import org.aktin.broker.auth.otp.repository.OperationResult;
import org.aktin.broker.auth.otp.repository.User;

/**
 * Defines the contract for managing user data and credential primitives.
 */
public interface UserService {

  /**
   * Return the user with the given username from the underlying persistence layer.
   *
   * @param username unique username
   * @return {@link User} or {@code null}
   */
  User get(String username);

  /**
   * Return all known users from the underlying persistence layer.
   *
   * @return list of users, never {@code null} but can be empty
   */
  List<User> list();

  /**
   * Create a new user with the given password. Expects a password as a plain {@code char[]}. The password is hashed before storage.
   *
   * @param username desired username
   * @param password password as {@code char[]}
   * @return An {@link OperationResult} indicating success or failure.
   */
  OperationResult create(String username, char[] password);

  /**
   * Activates a user account, allowing the user to log in.
   *
   * @param username The name of the user to activate.
   * @return An {@link OperationResult} indicating the outcome.
   */
  OperationResult activate(String username);

  /**
   * Deactivates a user account, preventing the user from logging in.
   *
   * @param username The name of the user to deactivate.
   * @return An {@link OperationResult} indicating the outcome.
   */
  OperationResult deactivate(String username);

  /**
   * Associates an OTP token's public identifier (binding) with a user account.
   * <p>
   * This method takes a full OTP token, derives a persistable public binding from it, and stores only that binding. The original token is not persisted. If no binding can be derived, the operation
   * will fail.
   *
   * @param username The name of the user.
   * @param token    The complete OTP token from which to derive the binding.
   * @return An {@link OperationResult} indicating the outcome.
   */
  OperationResult setToken(String username, String token);

  /**
   * Verifies if a provided plaintext password matches the user's stored password hash.
   *
   * @param user     The user entity against which to verify the password.
   * @param password The plaintext password to check.
   * @return {@code true} if the password is correct, {@code false} otherwise.
   */
  boolean verifyUserPassword(User user, char[] password);

  /**
   * Checks if the public identifier derived from a provided OTP token matches the one stored for the user.
   *
   * @param user  The user entity, which may have a stored token binding.
   * @param token The complete OTP token to check.
   * @return {@code true} if the binding matches or if the user has no binding, {@code false} otherwise.
   */
  boolean doesOtpBindingMatch(User user, String token);

  /**
   * Validates the correctness of a full OTP token, typically by communicating with an external provider.
   *
   * @param token The full OTP token to validate.
   * @return {@code true} if the token is valid, {@code false} otherwise.
   */
  boolean verifyOtpToken(String token);
}
