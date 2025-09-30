package org.aktin.broker.auth.otp.repository;

import java.util.List;
import java.util.Optional;

/**
 * Defines the contract for a persistence layer for {@link User} objects.
 */
public interface UserRepository {

  /**
   * Retrieves a single user by their unique username.
   *
   * @param username The name of the user to find.
   * @return The {@link User} object, or {@code null} if not found.
   */
  User find(String username);

  /**
   * Retrieves all stored user records.
   *
   * @return A {@link List} of all users, which may be empty but is never {@code null}.
   */
  List<User> findAll();

  /**
   * Creates and persists a new user record.
   *
   * @param username The unique username for the new user.
   * @param hash     The securely hashed password for the new user.
   * @return An {@link OperationResult} indicating the outcome (e.g., SUCCESS, USER_ALREADY_EXISTS).
   */
  OperationResult insert(String username, String hash);

  /**
   * Updates attributes of an existing user.
   * <p>
   * Implementations should only update the fields for which a non-null (or present Optional) value is provided. Null or empty parameters should be ignored, leaving the existing data unchanged.
   *
   * @param username The name of the user to update.
   * @param hash     The new password hash, or {@code null} to keep the existing one.
   * @param active   The new active status, or {@code null} to keep the existing one.
   * @param token    The new OTP token binding, or an empty Optional to keep the existing one.
   * @return An {@link OperationResult} indicating the outcome (e.g., SUCCESS, USER_NOT_FOUND).
   */
  OperationResult update(String username, String hash, Boolean active, Optional<String> token);
}
