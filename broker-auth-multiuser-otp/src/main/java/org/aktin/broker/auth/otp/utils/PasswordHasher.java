package org.aktin.broker.auth.otp.utils;

/**
 * Defines the contract for hashing and verifying passwords.
 */
public interface PasswordHasher {

  /**
   * Creates a secure hash of a plaintext password.
   *
   * @param password The plaintext password to hash, as a character array.
   * @return A formatted string containing the hash, salt, and algorithm parameters.
   */
  String hash(char[] password);

  /**
   * Compares a plaintext password with a stored hash to verify a match.
   *
   * @param password The plaintext password to check.
   * @param storedHash The hash string retrieved from storage.
   * @return {@code true} if the password matches the hash, {@code false} otherwise.
   */
  boolean verify(char[] password, String storedHash);
}
