package org.aktin.broker.auth.otp.utils;

import java.util.Optional;

public interface OtpProvider {

  Optional<String> deriveBinding(String token);

  boolean verify(String token);
}
