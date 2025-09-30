package org.aktin.broker.auth.otp.utils;

import java.util.Optional;

/**
 * Defines the contract for a One-Time Password (OTP) provider.
 */
public interface OtpProvider {

  /**
   * Derives a stable, public identifier (a "binding") from a full OTP token. This binding is intended to be stored to identify a user's specific OTP device or account.
   *
   * @param token The full OTP token provided by a client.
   * @return An {@link Optional} containing the derived binding, or an empty Optional if a binding cannot be derived from the token.
   */
  Optional<String> deriveBinding(String token);

  /**
   * Validates a full OTP token. This may involve communication with an external service.
   *
   * @param token The full OTP token to verify.
   * @return {@code true} if the token is valid, {@code false} otherwise.
   */
  boolean verify(String token);
}
