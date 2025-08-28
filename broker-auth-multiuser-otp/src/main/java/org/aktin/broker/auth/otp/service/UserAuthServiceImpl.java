package org.aktin.broker.auth.otp.service;

import java.util.Objects;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.aktin.broker.auth.otp.repository.OperationResult;
import org.aktin.broker.auth.otp.repository.User;

@Singleton
public class UserAuthServiceImpl implements UserAuthService {

  private static final Logger log = Logger.getLogger(UserAuthServiceImpl.class.getName());

  private static final String PROPERTY_ENFORCE_OTP = "aktin.broker.auth.enforce.otp";

  private final UserService userService;

  @Inject
  public UserAuthServiceImpl(UserService service) {
    this.userService = Objects.requireNonNull(service);
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
    if (!userService.verifyUserPassword(user, providedPassword)) {
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
        return true;
      } else {
        log.warning(String.format("Authentication failed for %s: could not store OTP token", username));
        return false;
      }
    }
    if (userHasOtp && hasToken) {
      if (!userService.doesOtpBindingMatch(user, token)) {
        log.info(String.format("Authentication failed for %s: OTP public ID mismatch", username));
        return false;
      }
      boolean otpValid = userService.verifyOtpToken(token);
      if (!otpValid) {
        log.info(String.format("Authentication failed for %s: invalid OTP token", username));
        return false;
      }
    }
    log.info(String.format("Authentication succeeded for %s", username));
    return true;
  }
}
