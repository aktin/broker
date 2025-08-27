package org.aktin.broker.auth.otp.utils;

import java.util.Optional;

public interface OtpProvider {

  String id();

  Optional<String> deriveBinding(String token);

  boolean verify(String token);
}
