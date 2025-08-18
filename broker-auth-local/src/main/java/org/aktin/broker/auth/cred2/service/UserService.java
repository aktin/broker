package org.aktin.broker.auth.cred2.service;

import java.sql.SQLException;
import java.util.List;
import org.aktin.broker.auth.cred2.repository.User;

public interface UserService {

  User get(String username) throws SQLException;

  List<User> list() throws SQLException;

  void create(String username, char[] password) throws SQLException;

  void activate(String username) throws SQLException;

  void deactivate(String username) throws SQLException;
}
