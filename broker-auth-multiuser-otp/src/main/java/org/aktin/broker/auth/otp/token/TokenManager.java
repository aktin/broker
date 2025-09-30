package org.aktin.broker.auth.otp.token;

/**
 * Defines the contract for managing the lifecycle of session tokens.
 */
public interface TokenManager {

  /**
   * Creates and returns a new session token for the specified user.
   *
   * @param username The name of the user for whom to issue the token.
   * @return A new {@link Token} instance.
   */
  Token issue(String username);

  /**
   * Retrieves an active and valid token by its unique identifier.
   *
   * @param guid The unique identifier (GUID) of the token to look up.
   * @return The {@link Token} if it exists and is valid, otherwise {@code null}.
   */
  Token lookup(String guid);

  /**
   * Invalidates a token, effectively terminating the user's session.
   *
   * @param guid The unique identifier (GUID) of the token to revoke.
   */
  void revoke(String guid);
}
