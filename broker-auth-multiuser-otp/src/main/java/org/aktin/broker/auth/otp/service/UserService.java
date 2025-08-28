package org.aktin.broker.auth.otp.service;

import java.util.List;
import org.aktin.broker.auth.otp.repository.OperationResult;
import org.aktin.broker.auth.otp.repository.User;

public interface UserService {

  User get(String username);

  List<User> list();

  OperationResult create(String username, char[] password);

  OperationResult activate(String username);

  OperationResult deactivate(String username);

  OperationResult setToken(String username, String token);

  boolean verifyUserPassword(User user, char[] password);

  boolean doesOtpBindingMatch(User user, String token);

  boolean verifyOtpToken(String token);
}
