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
}
