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
    if (password == null || password.isBlank()) {
      throw new IllegalStateException("Missing required property: " + PROPERTY_ADMIN_PASSWORD);
    }
    String hash = passwordHasher.hash(password.toCharArray());
    User initialUser = repository.find(username);
    if (initialUser == null) {
      repository.insert(username, hash);
    } else {
      repository.update(username, hash, null, Optional.empty());
    }
    log.info(String.format("Initialized default user %s from system properties", username));
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
    return repository.insert(username, hash);
  }

  @Override
  public OperationResult activate(String username) {
    return repository.update(username, null, true, Optional.empty());
  }

  @Override
  public OperationResult deactivate(String username) {
    return repository.update(username, null, false, Optional.empty());
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
    return repository.update(username, null, null, Optional.of(binding));
  }

  @Override
  public boolean verifyUserPassword(User user, char[] password) {
    if (user == null) {
      return false;
    }
    return passwordHasher.verify(password, user.password);
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
}
