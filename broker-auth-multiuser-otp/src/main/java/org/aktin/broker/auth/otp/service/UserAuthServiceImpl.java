package org.aktin.broker.auth.otp.service;

import java.util.Objects;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.aktin.broker.auth.otp.repository.User;
import org.aktin.broker.auth.otp.utils.PasswordHasher;

//TODO add option to enforce OTP
//TODO only verify OTP if user has OTP in db

@Singleton
public class UserAuthServiceImpl implements UserAuthService {

  private static final Logger log = Logger.getLogger(UserAuthServiceImpl.class.getName());

  private final UserService userService;
  private final PasswordHasher passwordHasher;

  @Inject
  public UserAuthServiceImpl(UserService service, PasswordHasher hasher) {
    this.userService = Objects.requireNonNull(service);
    this.passwordHasher = Objects.requireNonNull(hasher);
  }

  @Override
  public boolean authenticate(String username, char[] providedPassword) {
    log.info(String.format("Authenticating user: %s...", username));
    User user = userService.get(username);
    if (user == null) {
      log.info(String.format("User %s not found", username));
      return false;
    }
    if (!user.active) {
      log.info(String.format("User %s is inactive", username));
      return false;
    }
    if (!passwordHasher.algorithm().equalsIgnoreCase(user.algorithm)) {
      log.warning(String.format("User %s has unsupported algorithm: %s", username, user.algorithm));
      return false;
    }
    boolean ok = passwordHasher.verify(providedPassword, user.password);
    if (ok) {
      log.info(String.format("User %s accepted", username));
    } else {
      log.info(String.format("User %s denied", username));
    }
    return ok;
  }
}
