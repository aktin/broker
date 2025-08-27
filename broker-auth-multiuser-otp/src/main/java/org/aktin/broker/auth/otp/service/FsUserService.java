package org.aktin.broker.auth.otp.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.aktin.broker.auth.otp.repository.OperationResult;
import org.aktin.broker.auth.otp.repository.User;
import org.aktin.broker.auth.otp.repository.UserRepository;
import org.aktin.broker.auth.otp.utils.OtpProvider;
import org.aktin.broker.auth.otp.utils.PasswordHasher;

@Singleton
public class FsUserService implements UserService {

  private static final Logger log = Logger.getLogger(FsUserService.class.getName());

  private static final String PROPERTY_ADMIN_USER = "aktin.broker.username";
  private static final String DEFAULT_ADMIN_USER = "admin";
  private static final String PROPERTY_ADMIN_PASSWORD = "aktin.broker.password";

  private final UserRepository repository;
  private final PasswordHasher passwordHasher;
  private final OtpProvider otpProvider;

  @Inject
  public FsUserService(UserRepository repo, PasswordHasher hasher, OtpProvider otpProvider) {
    this.repository = Objects.requireNonNull(repo);
    this.passwordHasher = Objects.requireNonNull(hasher);
    this.otpProvider = Objects.requireNonNull(otpProvider);
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
  public OperationResult create(String username, char[] password) {
    String hash = passwordHasher.hash(password);
    return repository.insert(username, hash, passwordHasher.algorithm());
  }

  @Override
  public OperationResult activate(String username) {
    return repository.update(username, null, null, true, Optional.empty(), Optional.empty());
  }

  @Override
  public OperationResult deactivate(String username) {
    return repository.update(username, null, null, false, Optional.empty(), Optional.empty());
  }

  @Override
  public OperationResult setToken(String username, String token) {
    if (token == null || token.isBlank()) {
      return OperationResult.FAILED;
    }
    Optional<String> maybeBinding = otpProvider.deriveBinding(token);
    if (maybeBinding.isEmpty()) {
      return OperationResult.FAILED;
    }
    String binding = maybeBinding.get();
    return repository.update(username, null, null, null, Optional.of(otpProvider.id()), Optional.of(binding));
  }

  @Override
  public boolean verifyUserPassword(User user, char[] password) {
    if (user == null) {
      return false;
    }
    return passwordHasher.verify(password, user.password);
  }

  @Override
  public boolean isUserAlgorithmSupported(User user) {
    return user != null && passwordHasher.algorithm().equalsIgnoreCase(user.algorithm);
  }

  @Override
  public boolean doesOtpBindingMatch(User user, String token) {
    if (user == null || user.token.isEmpty() || token == null || token.isBlank()) {
      return false;
    }
    Optional<String> maybeBinding = otpProvider.deriveBinding(token);
    if (maybeBinding.isEmpty()) {
      return false;
    }
    String binding = maybeBinding.get();
    return user.token.get().equals(binding);
  }

  @Override
  public boolean verifyOtpToken(String token) {
    return otpProvider.verify(token);
  }

  @Override
  public boolean isOtpProviderSupported(User user) {
    if (user == null || user.tokenProvider.isEmpty()) {
      return false;
    }
    return otpProvider.id().equalsIgnoreCase(user.tokenProvider.get());
  }
}
