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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import javax.inject.Singleton;

/**
 * A file-system-based, thread-safe repository for user data.
 * <p>
 * This implementation stores user data in a plain text file and maintains a synchronized in-memory cache for fast read access. The path to the user file is configurable via the
 * {@code aktin.broker.users.file} system property.
 * <p>
 * A single {@link java.util.concurrent.locks.ReentrantReadWriteLock} is used to manage concurrency. This lock ensures thread-safe access to the in-memory cache and serializes write operations to
 * prevent corruption of the user file. The write lock is held during the entire transaction (cache update and file write) to guarantee consistency between the cache and the file system.
 */
@Singleton
public class FsUserRepository implements UserRepository {

  private static final Logger log = Logger.getLogger(FsUserRepository.class.getName());

  private static final String PROPERTY_USER_FILE = "aktin.broker.users.file";
  private static final String DEFAULT_USER_FILE = "users.txt";
  private static final String FIELD_SEPARATOR = "\t";
  private static final int EXPECTED_FIELD_COUNT = 5;

  private final Path usersFile;
  private final Map<String, User> userCache = new HashMap<>();
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

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
        throw new IllegalStateException("Could not create users file: " + usersFile, e);
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
        if (parts.length == EXPECTED_FIELD_COUNT) {
          User user = parseUser(parts);
          userCache.put(user.username, user);
          loadedCount++;
        } else {
          log.warning(String.format("Skipping malformed line in %s: expected %d fields, got %d", usersFile.getFileName(), EXPECTED_FIELD_COUNT, parts.length));
        }
      }
      log.info(String.format("Loaded %d users into cache from %s", loadedCount, usersFile.getFileName()));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load users from file: " + usersFile, e);
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

  /**
   * Inserts a new user by atomically updating the cache and writing the changes to the file.
   */
  @Override
  public OperationResult insert(String username, String hash) {
    lock.writeLock().lock();
    try {
      if (userCache.containsKey(username)) {
        return OperationResult.USER_ALREADY_EXISTS;
      }
      long createdAt = System.currentTimeMillis();
      User user = new User(username, hash, true, createdAt, Optional.empty());
      userCache.put(username, user);
      saveUsersToFile(lock);
      return OperationResult.SUCCESS;
    } catch (IOException e) {
      log.severe(String.format("Failed to insert user %s: %s", username, e.getMessage()));
      throw new RuntimeException("Failed to save user data", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Updates a user by atomically updating the cache and writing the changes to the file.
   */
  @Override
  public OperationResult update(String username, String hash, Boolean active, Optional<String> token) {
    lock.writeLock().lock();
    try {
      User existingUser = userCache.get(username);
      if (existingUser == null) {
        return OperationResult.USER_NOT_FOUND;
      }
      String newHash = (hash != null) ? hash : existingUser.password;
      boolean newActive = (active != null) ? active : existingUser.active;
      Optional<String> newToken = token.isPresent() ? token : existingUser.token;
      User updatedUser = new User(username, newHash, newActive, existingUser.createdAt, newToken);
      userCache.put(username, updatedUser);
      saveUsersToFile(lock);
      return OperationResult.SUCCESS;
    } catch (IOException e) {
      log.severe(String.format("Failed to update user %s: %s", username, e.getMessage()));
      throw new RuntimeException("Failed to save user data", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Saves the current user cache to the file, sorted by username. This method requires the caller to hold the write lock to ensure file integrity.
   *
   * @param rw The lock instance, used to verify that the write lock is held.
   * @throws IOException           if the file cannot be written to.
   * @throws IllegalStateException if the current thread does not hold the write lock.
   */
  private void saveUsersToFile(ReentrantReadWriteLock rw) throws IOException {
    if (!rw.isWriteLockedByCurrentThread()) {
      throw new IllegalStateException("saveUsersToFile requires the write lock");
    }
    List<String> lines = new ArrayList<>();
    userCache.values().stream()
        .sorted(Comparator.comparing(u -> u.username))
        .forEach(user -> lines.add(formatUserLine(user)));
    Files.write(usersFile, lines);
  }

  private User parseUser(String[] parts) {
    String username = parts[0];
    String password = parts[1];
    boolean active = Boolean.parseBoolean(parts[2]);
    long createdAt = Long.parseLong(parts[3]);
    String tokenStr = parts[4];
    Optional<String> token = tokenStr.isEmpty() ? Optional.empty() : Optional.of(tokenStr);
    return new User(username, password, active, createdAt, token);
  }

  private String formatUserLine(User user) {
    return String.join(FIELD_SEPARATOR,
        user.username,
        user.password,
        String.valueOf(user.active),
        String.valueOf(user.createdAt),
        user.token.orElse(""));
  }
}
