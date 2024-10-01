package org.aktin.broker.auth.apikey;

/**
 * Enum representing the status of an API key in the AKTIN broker authentication system.
 *
 * @author akombeiz@ukaachen.de
 */
public enum ApiKeyStatus {
  /**
   * Represents an active API key. This status is considered valid and operational within the system.
   */
  ACTIVE,

  /**
   * Represents an inactive API key. An API key with this status cannot be used for authentication purposes. Typically used for keys that have been
   * revoked, expired, or temporarily suspended.
   */
  INACTIVE;

}
