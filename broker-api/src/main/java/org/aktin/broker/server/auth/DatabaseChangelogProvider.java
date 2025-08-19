package org.aktin.broker.server.auth;

/**
 * Hook for {@link AuthProvider} that need their own DB schema
 * <p>
 * Implement this and ship a Liquibase changelog on the classpath
 * <p>
 * The broker will run it after the core schema
 */
public interface DatabaseChangelogProvider {

  String getChangeLogPath();
}
