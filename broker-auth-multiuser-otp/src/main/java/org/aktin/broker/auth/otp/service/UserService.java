package org.aktin.broker.auth.otp.service;

import java.util.List;
import org.aktin.broker.auth.otp.repository.User;

public interface UserService {

  User get(String username);

  List<User> list();

  boolean create(String username, char[] password);

  boolean activate(String username);

  boolean deactivate(String username);

  boolean setToken(String username, String token);
}
