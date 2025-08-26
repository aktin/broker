package org.aktin.broker.auth.otp.repository;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

  User find(String username);

  List<User> findAll();

  OperationResult insert(String username, String hash, String algorithm);

  OperationResult update(String username, String hash, String algorithm, Boolean active, Optional<String> token);
}
