package org.aktin.broker.auth.cred2.service;

import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.aktin.broker.auth.cred2.repository.User;
import org.aktin.broker.auth.cred2.utils.PasswordHasher;

@Singleton
class JdbcUserAuthService implements UserAuthService {

  private static final Logger log = Logger.getLogger(UserAuthService.class.getName());

  private final UserService userService;
  private final PasswordHasher passwordHasher;

  @Inject
  public JdbcUserAuthService(UserService service, PasswordHasher hasher) {
    this.userService = Objects.requireNonNull(service);
    this.passwordHasher = Objects.requireNonNull(hasher);
  }

  @Override
  public boolean authenticate(String username, char[] providedPassword) {
    log.info(String.format("Authenticating user: %s...", username));
    try {
      User user = userService.get(username);
      if (user == null) {
        log.info("User not found");
        return false;
      }
      if (!user.active) {
        log.info("User is inactive");
        return false;
      }
      if (!passwordHasher.algorithm().equalsIgnoreCase(user.algorithm)) {
        log.warning(String.format("User has unsupported algorithm: %s", user.algorithm));
        return false;
      }
      boolean ok = passwordHasher.verify(providedPassword, user.password);
      if (ok) {
        log.info("User accepted");
      } else {
        log.info("User denied");
      }
      return ok;
    } catch (SQLException e) {
      log.severe(String.format("SQL error for user %s: %s", username, e.getMessage()));
      return false;
    }
  }
}
