package org.aktin.broker.auth.cred2.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.aktin.broker.auth.cred2.repository.User;
import org.aktin.broker.auth.cred2.repository.UserRepository;
import org.aktin.broker.auth.cred2.utils.PasswordHasher;

@Singleton
class JdbcUserService implements UserService {

  private static final Logger log = Logger.getLogger(JdbcUserService.class.getName());

  private static final String PROPERTY_ADMIN_USER = "aktin.broker.username";
  private static final String DEFAULT_ADMIN_USER = "admin";
  private static final String PROPERTY_ADMIN_PASSWORD = "aktin.broker.password";

  private final DataSource dataSource;
  private final UserRepository repository;
  private final PasswordHasher passwordHasher;

  @Inject
  public JdbcUserService(DataSource ds, UserRepository repo, PasswordHasher hasher) {
    this.dataSource = Objects.requireNonNull(ds);
    this.repository = Objects.requireNonNull(repo);
    this.passwordHasher = Objects.requireNonNull(hasher);
  }

  @Override
  public void initializeDefaultUser() throws SQLException {
    String username = System.getProperty(PROPERTY_ADMIN_USER, DEFAULT_ADMIN_USER);
    String password = System.getProperty(PROPERTY_ADMIN_PASSWORD);
    try (Connection c = dataSource.getConnection()) {
      User initialUser = repository.find(c, username);
      if (initialUser == null) {
        createDefaultUser(c, username, password);
      } else {
        updateDefaultUser(c, username, password);
      }
    }
  }

  private void createDefaultUser(Connection c, String username, String password) throws SQLException {
    String raw;
    if (password == null || password.isBlank()) {
      raw = generateRandomPassword();
      log.info(String.format("Creating initial user %s with random password %s", username, raw));
    } else {
      raw = password;
      log.info(String.format("Creating initial user %s from system properties", username));
    }
    String hash = passwordHasher.hash(raw.toCharArray());
    repository.insert(c, username, hash, passwordHasher.algorithm());
  }

  private void updateDefaultUser(Connection c, String username, String password) throws SQLException {
    if (password == null || password.isBlank()) {
      return;
    }
    log.info(String.format("Updating initial user %s from system properties", username));
    String hash = passwordHasher.hash(password.toCharArray());
    repository.update(c, username, hash, passwordHasher.algorithm());
  }

  @Override
  public User get(String username) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      return repository.find(c, username);
    }
  }

  @Override
  public List<User> list() throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      return repository.findAll(c);
    }
  }

  @Override
  public void create(String username, char[] password) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      String hash = passwordHasher.hash(password);
      repository.insert(c, username, hash, passwordHasher.algorithm());
    }
  }

  @Override
  public void activate(String username) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      repository.activate(c, username);
    }
  }

  @Override
  public void deactivate(String username) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      repository.deactivate(c, username);
    }
  }

  private static String generateRandomPassword() {
    var b = new StringBuilder(12);
    final String alpha = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz1234567890@#!$%&=?";
    for (int i = 0; i < 16; i++) {
      int idx = (int) (Math.random() * alpha.length());
      b.append(alpha.charAt(idx));
    }
    return b.toString();
  }
}
