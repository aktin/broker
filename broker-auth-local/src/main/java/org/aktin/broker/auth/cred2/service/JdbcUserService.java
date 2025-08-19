package org.aktin.broker.auth.cred2.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.aktin.broker.auth.cred2.repository.User;
import org.aktin.broker.auth.cred2.repository.UserRepository;
import org.aktin.broker.auth.cred2.utils.PasswordHasher;

@Singleton
public class JdbcUserService implements UserService {

  private static final Logger log = Logger.getLogger(JdbcUserService.class.getName());

  private static final String PROPERTY_ADMIN_USER = "aktin.broker.username";
  private static final String DEFAULT_ADMIN_USER = "admin";
  private static final String PROPERTY_ADMIN_PASSWORD = "aktin.broker.password";

  private final DataSource dataSource;
  private final UserRepository repository;
  private final PasswordHasher passwordHasher;

  @Inject
  public JdbcUserService(UserRepository repo, PasswordHasher hasher) {
    this.dataSource = initializeDataSourceFromSystemProperties();
    this.repository = Objects.requireNonNull(repo);
    this.passwordHasher = Objects.requireNonNull(hasher);
    try {
      initializeDefaultUser();
    } catch (SQLException e) {
      log.log(Level.SEVERE, "Initialization of default user failed", e);
    }
  }

  // dirty, rebuilds manually datasource like DefaultConfiguration.java
  private DataSource initializeDataSourceFromSystemProperties() {
    String dsClassName = "org.hsqldb.jdbc.JDBCDataSource";
    Path basePath = Paths.get(".");
    String path = basePath.resolve("broker").toString();
    String url = "jdbc:hsqldb:file:" + path + ";shutdown=false;user=admin;password=secret";
    try {
      Class<?> clazz = Class.forName(dsClassName);
      Object ds = clazz.getConstructor().newInstance();
      clazz.getMethod("setURL", String.class).invoke(ds, url);
      return (DataSource) ds;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create DataSource from system properties", e);
    }
  }

  private void initializeDefaultUser() throws SQLException {
    String username = System.getProperty(PROPERTY_ADMIN_USER, DEFAULT_ADMIN_USER);
    String password = System.getProperty(PROPERTY_ADMIN_PASSWORD);
    try (Connection c = dataSource.getConnection()) {
      User initialUser = repository.find(c, username);
      if (initialUser == null) {
        createDefaultUser(c, username, password);
      } else {
        updateDefaultUser(c, initialUser, password);
      }
    }
  }

  private void createDefaultUser(Connection c, String username, String password) throws SQLException {
    String raw;
    if (password == null || password.isBlank()) {
      raw = generateRandomPassword();
      log.info(String.format("Creating default user %s with random password %s", username, raw));
    } else {
      raw = password;
      log.info(String.format("Creating default user %s from system properties", username));
    }
    String hash = passwordHasher.hash(raw.toCharArray());
    repository.insert(c, username, hash, passwordHasher.algorithm());
  }

  private void updateDefaultUser(Connection c, User user, String password) throws SQLException {
    if (password == null || password.isBlank()) {
      log.info("No password provided for default user. Skipping password update.");
      return;
    }
    String newHash = passwordHasher.hash(password.toCharArray());
    if (newHash.equals(user.password) && passwordHasher.algorithm().equalsIgnoreCase(user.algorithm)) {
      return;
    }
    log.info(String.format("Updating default user %s from system properties", user.username));
    repository.update(c, user.username, newHash, passwordHasher.algorithm());
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
    log.info(String.format("New user created: %s (alg=%s)", username, passwordHasher.algorithm()));
  }

  @Override
  public void activate(String username) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      repository.activate(c, username);
    }
    log.info("User activated: " + username);
  }

  @Override
  public void deactivate(String username) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      repository.deactivate(c, username);
    }
    log.info("User deactivated: " + username);
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