package org.aktin.broker.auth.otp.utils;

public interface OtpVerificationService {

  boolean verify(String token);
}
