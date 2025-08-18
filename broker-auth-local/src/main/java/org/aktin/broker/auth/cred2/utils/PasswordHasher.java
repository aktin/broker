package org.aktin.broker.auth.cred2.utils;

public interface PasswordHasher {

  String algorithm();

  String hash(char[] password);

  boolean verify(char[] password, String storedHash);
}
