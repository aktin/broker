package org.aktin.broker.auth.otp.service;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.aktin.broker.auth.otp.repository.User;
import org.aktin.broker.auth.otp.repository.UserRepository;
import org.aktin.broker.auth.otp.utils.PasswordHasher;

@Singleton
public class FsUserService implements UserService {

  private static final Logger log = Logger.getLogger(FsUserService.class.getName());

  private static final String PROPERTY_ADMIN_USER = "aktin.broker.username";
  private static final String DEFAULT_ADMIN_USER = "admin";
  private static final String PROPERTY_ADMIN_PASSWORD = "aktin.broker.password";

  private final UserRepository repository;
  private final PasswordHasher passwordHasher;

  @Inject
  public FsUserService(UserRepository repo, PasswordHasher hasher) {
    this.repository = Objects.requireNonNull(repo);
    this.passwordHasher = Objects.requireNonNull(hasher);
    initializeDefaultUser();
  }

  private void initializeDefaultUser() {
    String username = System.getProperty(PROPERTY_ADMIN_USER, DEFAULT_ADMIN_USER);
    String password = System.getProperty(PROPERTY_ADMIN_PASSWORD);
    User initialUser = repository.find(username);
    if (initialUser == null) {
      String raw;
      if (password == null || password.isBlank()) {
        raw = generateRandomPassword();
        log.info(String.format("Creating default user %s with random password %s", username, raw));
      } else {
        raw = password;
        log.info(String.format("Creating default user %s from system properties", username));
      }
      String hash = passwordHasher.hash(raw.toCharArray());
      repository.insert(username, hash, passwordHasher.algorithm());
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

  @Override
  public User get(String username) {
    return repository.find(username);
  }

  @Override
  public List<User> list() {
    return repository.findAll();
  }

  @Override
  public boolean create(String username, char[] password) {
    String hash = passwordHasher.hash(password);
    return repository.insert(username, hash, passwordHasher.algorithm());
  }

  @Override
  public boolean activate(String username) {
    return repository.activate(username);
  }

  @Override
  public boolean deactivate(String username) {
    return repository.deactivate(username);
  }
}