package org.aktin.broker.auth.apikey;

/**
 * Enum representing the status of an API key.
 */
public enum ApiKeyStatus {
  ACTIVE,
  INACTIVE;

  @Override
  public String toString() {
    return name().toLowerCase();
  }

  public static ApiKeyStatus fromString(String value) {
    for (ApiKeyStatus status : ApiKeyStatus.values()) {
      if (status.name().equalsIgnoreCase(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown status: " + value);
  }
}
