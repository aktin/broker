package org.aktin.broker.auth.otp.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import javax.inject.Singleton;

@Singleton
public class FsUserRepository implements UserRepository {

  private static final Logger log = Logger.getLogger(FsUserRepository.class.getName());

  private static final String PROPERTY_USER_FILE = "aktin.broker.user.file";
  private static final String DEFAULT_USER_FILE = "users.txt";
  private static final String FIELD_SEPARATOR = ";";

  private final Path usersFile;
  private final Map<String, User> userCache = new ConcurrentHashMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private volatile boolean cacheLoaded = false;

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
        throw new RuntimeException("Could not create users file: " + e.getMessage());
      }
    }
  }

  private void loadUsersIntoCache() {
    lock.writeLock().lock();
    try {
      userCache.clear();
      List<String> lines = Files.readAllLines(usersFile);
      int loadedCount = 0;
      for (String line : lines) {
        if (line == null || line.trim().isEmpty()) {
          continue;
        }
        String[] parts = line.split(FIELD_SEPARATOR, -1);
        if (parts.length >= 5) {
          User user = parseUser(parts);
          userCache.put(user.username, user);
          loadedCount++;
        }
      }
      cacheLoaded = true;
      log.info(String.format("Loaded %d users into cache from %s", loadedCount, usersFile.getFileName()));
    } catch (IOException e) {
      log.severe("Failed to load users from file: " + e.getMessage());
      cacheLoaded = true;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void saveUsersToFile() {
    try {
      List<String> lines = new ArrayList<>();
      userCache.values().stream()
          .sorted(Comparator.comparing(u -> u.username))
          .forEach(user -> lines.add(formatUserLine(user)));
      Files.write(usersFile, lines);
    } catch (IOException e) {
      throw new RuntimeException("Failed to persist user data: " + e.getMessage());
    }
  }

  @Override
  public User find(String username) {
    ensureCacheLoaded();
    lock.readLock().lock();
    try {
      return userCache.get(username);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public List<User> findAll() {
    ensureCacheLoaded();
    lock.readLock().lock();
    try {
      return new ArrayList<>(userCache.values());
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean insert(String username, String hash, String algorithm) {
    ensureCacheLoaded();
    lock.writeLock().lock();
    try {
      if (userCache.containsKey(username)) {
        log.warning("User already exists: " + username);
        return false;
      }
      long createdAt = System.currentTimeMillis();
      User user = new User(username, hash, algorithm, true, createdAt);
      userCache.put(username, user);
      saveUsersToFile();
      log.info("User created: " + username);
      return true;
    } catch (Exception e) {
      log.severe(String.format("Failed to insert user %s: %s", username, e.getMessage()));
      return false;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean updatePassword(String username, String hash, String algorithm) {
    return updateUser(username, hash, algorithm, null);
  }

  @Override
  public boolean activate(String username) {
    return updateUser(username, null, null, true);
  }

  @Override
  public boolean deactivate(String username) {
    return updateUser(username, null, null, false);
  }

  private boolean updateUser(String username, String hash, String algorithm, Boolean active) {
    ensureCacheLoaded();
    lock.writeLock().lock();
    try {
      User existingUser = userCache.get(username);
      if (existingUser == null) {
        log.warning("User not found for update: " + username);
        return false;
      }
      String newHash = (hash != null) ? hash : existingUser.password;
      String newAlg = (algorithm != null) ? algorithm : existingUser.algorithm;
      boolean newActive = (active != null) ? active : existingUser.active;
      User updatedUser = new User(username, newHash, newAlg, newActive, existingUser.createdAt);
      userCache.put(username, updatedUser);
      saveUsersToFile();
      log.info("User updated: " + username);
      return true;
    } catch (Exception e) {
      log.severe(String.format("Failed to update user %s: %s", username, e.getMessage()));
      return false;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void ensureCacheLoaded() {
    if (!cacheLoaded) {
      loadUsersIntoCache();
    }
  }

  private User parseUser(String[] parts) {
    String username = parts[0];
    String password = parts[1];
    String algorithm = parts[2];
    boolean active = Boolean.parseBoolean(parts[3]);
    long createdAt = Long.parseLong(parts[4]);
    return new User(username, password, algorithm, active, createdAt);
  }

  private String formatUserLine(User user) {
    return String.join(FIELD_SEPARATOR,
        user.username,
        user.password,
        user.algorithm,
        String.valueOf(user.active),
        String.valueOf(user.createdAt));
  }
}
