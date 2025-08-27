package org.aktin.broker.auth.otp.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import javax.inject.Singleton;

@Singleton
public class FsUserRepository implements UserRepository {

  private static final Logger log = Logger.getLogger(FsUserRepository.class.getName());

  private static final String PROPERTY_USER_FILE = "aktin.broker.users.file";
  private static final String DEFAULT_USER_FILE = "users.txt";
  private static final String FIELD_SEPARATOR = "\t";

  private final Path usersFile;
  private final Map<String, User> userCache = new HashMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  public FsUserRepository() {
    this(System.getProperty(PROPERTY_USER_FILE, DEFAULT_USER_FILE));
  }

  public FsUserRepository(String userFilePath) {
    this.usersFile = Paths.get(userFilePath);
    initUsersFileIfNonexisting();
    loadUsersIntoCache();
  }

  private void initUsersFileIfNonexisting() {
    if (!Files.exists(usersFile)) {
      try {
        if (usersFile.getParent() != null) {
          Files.createDirectories(usersFile.getParent());
        }
        Files.createFile(usersFile);
        log.info("Created users file: " + usersFile.toAbsolutePath());
      } catch (IOException e) {
        log.severe("Could not create users file: " + e.getMessage());
      }
    }
  }

  private void loadUsersIntoCache() {
    try {
      List<String> lines = Files.readAllLines(usersFile);
      int loadedCount = 0;
      for (String line : lines) {
        if (line == null || line.trim().isEmpty()) {
          continue;
        }
        String[] parts = line.split(FIELD_SEPARATOR, -1);
        if (parts.length == 7) {
          User user = parseUser(parts);
          userCache.put(user.username, user);
          loadedCount++;
        } else {
          log.warning(String.format("Skipping malformed line in %s: expected 7 fields, got %d", usersFile.getFileName(), parts.length));
        }
      }
      log.info(String.format("Loaded %d users into cache from %s", loadedCount, usersFile.getFileName()));
    } catch (IOException e) {
      log.severe("Failed to load users from file: " + e.getMessage());
    }
  }

  @Override
  public User find(String username) {
    lock.readLock().lock();
    try {
      return userCache.get(username);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public List<User> findAll() {
    lock.readLock().lock();
    try {
      return new ArrayList<>(userCache.values());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public OperationResult insert(String username, String hash, String algorithm) {
    lock.writeLock().lock();
    try {
      if (userCache.containsKey(username)) {
        return OperationResult.USER_ALREADY_EXISTS;
      }
      long createdAt = System.currentTimeMillis();
      User user = new User(username, hash, algorithm, true, createdAt, Optional.empty(), Optional.empty());
      userCache.put(username, user);
      saveUsersToFile((ReentrantReadWriteLock) lock);
      return OperationResult.SUCCESS;
    } catch (Exception e) {
      log.severe(String.format("Failed to insert user %s: %s", username, e.getMessage()));
      return OperationResult.FAILED;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public OperationResult update(String username, String hash, String algorithm, Boolean active, Optional<String> provider, Optional<String> token) {
    lock.writeLock().lock();
    try {
      User existingUser = userCache.get(username);
      if (existingUser == null) {
        return OperationResult.USER_NOT_FOUND;
      }
      String newHash = (hash != null) ? hash : existingUser.password;
      String newAlg = (algorithm != null) ? algorithm : existingUser.algorithm;
      boolean newActive = (active != null) ? active : existingUser.active;
      Optional<String> newProvider = provider.isPresent() ? provider : existingUser.tokenProvider;
      Optional<String> newToken = token.isPresent() ? token : existingUser.token;
      User updatedUser = new User(username, newHash, newAlg, newActive, existingUser.createdAt, newProvider, newToken);
      userCache.put(username, updatedUser);
      saveUsersToFile((ReentrantReadWriteLock) lock);
      return OperationResult.SUCCESS;
    } catch (Exception e) {
      log.severe(String.format("Failed to update user %s: %s", username, e.getMessage()));
      return OperationResult.FAILED;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void saveUsersToFile(ReentrantReadWriteLock rw) {
    if (!rw.isWriteLockedByCurrentThread()) {
      throw new IllegalStateException("saveUsersToFile requires the write lock");
    }
    try {
      List<String> lines = new ArrayList<>();
      userCache.values().stream()
          .sorted(Comparator.comparing(u -> u.username))
          .forEach(user -> lines.add(formatUserLine(user)));
      Files.write(usersFile, lines);
    } catch (IOException e) {
      log.severe("Failed to persist user data: " + e.getMessage());
    }
  }

  private User parseUser(String[] parts) {
    String username = parts[0];
    String password = parts[1];
    String algorithm = parts[2];
    boolean active = Boolean.parseBoolean(parts[3]);
    long createdAt = Long.parseLong(parts[4]);
    String providerStr = parts[5];
    Optional<String> provider = providerStr.isEmpty() ? Optional.empty() : Optional.of(providerStr);
    String tokenStr = parts[6];
    Optional<String> token = tokenStr.isEmpty() ? Optional.empty() : Optional.of(tokenStr);
    return new User(username, password, algorithm, active, createdAt, provider, token);
  }

  private String formatUserLine(User user) {
    return String.join(FIELD_SEPARATOR,
        user.username,
        user.password,
        user.algorithm,
        String.valueOf(user.active),
        String.valueOf(user.createdAt),
        user.tokenProvider.orElse(""),
        user.token.orElse(""));
  }
}
