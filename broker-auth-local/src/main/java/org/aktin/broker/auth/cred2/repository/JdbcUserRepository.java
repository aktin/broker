package org.aktin.broker.auth.cred2.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Singleton;

@Singleton
public class JdbcUserRepository implements UserRepository {

  @Override
  public User find(Connection c, String username) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("SELECT username, password, algorithm, is_active, created_at FROM users WHERE username = ?")) {
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
    try (PreparedStatement ps = c.prepareStatement("SELECT username, password, algorithm, is_active, created_at FROM users ORDER BY username");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        out.add(map(rs));
      }
    }
    return out;
  }

  @Override
  public void insert(Connection c, String username, String hash, String alg) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("INSERT INTO users (username, password, algorithm, is_active, created_at) VALUES (?, ?, ?, TRUE, CURRENT_TIMESTAMP)")) {
      ps.setString(1, username);
      ps.setString(2, hash);
      ps.setString(3, alg);
      ps.executeUpdate();
    }
  }

  @Override
  public void update(Connection c, String username, String hash, String alg) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("UPDATE users SET password = ?, algorithm = ? WHERE username = ?")) {
      ps.setString(1, hash);
      ps.setString(2, alg);
      ps.setString(3, username);
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
    String alg = rs.getString("algorithm");
    boolean active = rs.getBoolean("is_active");
    Timestamp ts = rs.getTimestamp("created_at");
    Instant created = ts.toInstant();
    return new User(username, hash, alg, active, created);
  }
}
