package org.aktin.broker.auth.cred2;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface UserDao {

  User find(Connection c, String username) throws SQLException;

  List<User> findAll(Connection c) throws SQLException;

  void insert(Connection c, String username, String hash, String alg) throws SQLException;

  void activate(Connection c, String username) throws SQLException;

  void deactivate(Connection c, String username) throws SQLException;
}
