package org.aktin.broker.auth.otp.utils;

public interface PasswordHasher {

  String hash(char[] password);

  boolean verify(char[] password, String storedHash);
}
