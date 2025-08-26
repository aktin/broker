package org.aktin.broker.auth.otp.repository;

import java.util.List;

public interface UserRepository {

  User find(String username);

  List<User> findAll();

  boolean insert(String username, String hash, String algorithm);

  boolean activate(String username);

  boolean deactivate(String username);

  boolean setToken(String username, String token);
}
