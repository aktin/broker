package org.aktin.broker.auth.cred2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserDaoJdbc implements UserDao {

  @Override
  public User find(Connection c, String username) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("SELECT username, password, alg, is_active, created_at FROM users WHERE username = ?")) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        return map(rs);
      }
    }
  }

  @Override
  public List<User> findAll(Connection c) throws SQLException {
    List<User> out = new ArrayList<>();
    try (PreparedStatement ps = c.prepareStatement("SELECT username, password, alg, is_active, created_at FROM users ORDER BY username");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        out.add(map(rs));
      }
    }
    return out;
  }

  @Override
  public void insert(Connection c, String username, String hash, String alg) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO users (username, password, alg, is_active, created_at) VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP)")) {
      ps.setString(1, username);
      ps.setString(2, hash);
      ps.setString(3, alg);
      ps.executeUpdate();
    }
  }

  @Override
  public void activate(Connection c, String username) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("UPDATE users SET is_active = TRUE WHERE username = ?")) {
      ps.setString(1, username);
      ps.executeUpdate();
    }
  }

  @Override
  public void deactivate(Connection c, String username) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("UPDATE users SET is_active = FALSE WHERE username = ?")) {
      ps.setString(1, username);
      ps.executeUpdate();
    }
  }

  private static User map(ResultSet rs) throws SQLException {
    String username = rs.getString("username");
    String hash = rs.getString("password");
    String alg = rs.getString("alg");
    boolean active = rs.getBoolean("is_active");
    Timestamp ts = rs.getTimestamp("created_at");
    Instant created = ts.toInstant();
    return new User(username, hash, alg, active, created);
  }
}
