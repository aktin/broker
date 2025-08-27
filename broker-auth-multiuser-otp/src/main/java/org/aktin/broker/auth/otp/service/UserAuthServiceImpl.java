package org.aktin.broker.auth.otp.service;

import java.util.Objects;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.aktin.broker.auth.otp.repository.OperationResult;
import org.aktin.broker.auth.otp.repository.User;
import org.aktin.broker.auth.otp.utils.OtpVerificationService;
import org.aktin.broker.auth.otp.utils.PasswordHasher;

@Singleton
public class UserAuthServiceImpl implements UserAuthService {

  private static final Logger log = Logger.getLogger(UserAuthServiceImpl.class.getName());

  private static final String PROPERTY_ENFORCE_OTP = "aktin.broker.auth.enforce.otp";

  private final UserService userService;
  private final PasswordHasher passwordHasher;
  private final OtpVerificationService otpVerificationService;

  @Inject
  public UserAuthServiceImpl(UserService service, PasswordHasher hasher, OtpVerificationService otpVerificationService) {
    this.userService = Objects.requireNonNull(service);
    this.passwordHasher = Objects.requireNonNull(hasher);
    this.otpVerificationService = Objects.requireNonNull(otpVerificationService);
  }

  @Override
  public boolean authenticate(String username, char[] providedPassword, String token) {
    User user = userService.get(username);
    if (user == null) {
      log.info(String.format("Authentication failed for %s: user not found", username));
      return false;
    }
    if (!user.active) {
      log.info(String.format("Authentication failed for %s: user inactive", username));
      return false;
    }
    if (!passwordHasher.algorithm().equalsIgnoreCase(user.algorithm)) {
      log.warning(String.format("Authentication failed for %s: unsupported algorithm %s", username, user.algorithm));
      return false;
    }
    boolean passwordValid = passwordHasher.verify(providedPassword, user.password);
    if (!passwordValid) {
      log.info(String.format("Authentication failed for %s: invalid password", username));
      return false;
    }

    boolean otpEnforced = Boolean.parseBoolean(System.getProperty(PROPERTY_ENFORCE_OTP, "false"));
    boolean userHasOtp = user.token.isPresent();
    boolean hasToken = token != null && !token.trim().isEmpty();

    if (otpEnforced || userHasOtp) {
      if (!hasToken) {
        log.info(String.format("Authentication failed for %s: OTP token required", username));
        return false;
      }
    }
    if (!userHasOtp && hasToken) {
      OperationResult result = userService.setToken(username, token);
      if (result == OperationResult.SUCCESS) {
        log.info(String.format("Authentication succeeded for %s: password valid, assigned first-time OTP token", username));
        return true; // password already validated, OTP now registered
      } else {
        log.warning(String.format("Authentication failed for %s: could not store OTP token", username));
        return false;
      }
    }
    if (userHasOtp && hasToken) {
      if (!isPublicIdValid(user, token)) {
        log.info(String.format("Authentication failed for %s: OTP public ID mismatch", username));
        return false;
      }
      boolean otpValid = otpVerificationService.verify(token);
      if (!otpValid) {
        log.info(String.format("Authentication failed for %s: invalid OTP token", username));
        return false;
      }
    }
    log.info(String.format("Authentication succeeded for %s", username));
    return true;
  }

  private boolean isPublicIdValid(User user, String token) {
    if (token.length() < 12) {
      return false;
    }
    String expectedPublicId = user.token.get();
    String actualPublicId = token.substring(0, 12);
    return expectedPublicId.equals(actualPublicId);
  }
}
