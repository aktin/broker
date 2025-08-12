package org.aktin.broker.server.auth;

/**
 * Hook for auth providers that need their own DB schema
 * Implement this and ship a Liquibase changelog on the classpath
 * The broker will run it after the core schema
 */
public interface DatabaseChangelogProvider {

  String getChangeLogPath();
}
