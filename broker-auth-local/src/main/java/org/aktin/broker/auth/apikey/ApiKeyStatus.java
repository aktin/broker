package org.aktin.broker.auth.apikey;

/**
 * Enum representing the status of an API key.
 */
public enum ApiKeyStatus {
  /**
   * Represents an active API key. An active key is fully operational and can be used for authentication.
   */
  ACTIVE,

  /**
   * Represents an inactive API key. An inactive key is not operational and cannot be used for authentication.
   */
  INACTIVE;

  @Override
  public String toString() {
    return name().toLowerCase();
  }

  /**
   * Converts a string to the corresponding ApiKeyStatus enum constant.
   *
   * @param value The string representation of the API key status.
   * @return The corresponding ApiKeyStatus enum constant.
   * @throws IllegalArgumentException if the input string does not match any defined ApiKeyStatus (case-insensitive).
   */
  public static ApiKeyStatus fromString(String value) {
    for (ApiKeyStatus status : ApiKeyStatus.values()) {
      if (status.name().equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown status: " + value);
  }
}