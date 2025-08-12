package org.aktin.broker.auth.cred2;

import java.io.IOException;
import java.util.function.BiConsumer;
import org.aktin.broker.server.auth.AbstractAuthProvider;
import org.aktin.broker.server.auth.DatabaseChangelogProvider;

public class CredentialTokenAuthProvider extends AbstractAuthProvider implements DatabaseChangelogProvider {

  private TokenManager manager;
  private CredentialTokenAuth auth;

  public static final String CHANGELOG_RESOURCE = "userCreds.xml";

  public CredentialTokenAuthProvider(String simplePassword) {
    this.manager = new TokenManager(simplePassword);
    this.auth = new CredentialTokenAuth(manager);
  }

  public CredentialTokenAuthProvider() {
    this.manager = new TokenManager();
    this.auth = new CredentialTokenAuth(manager);
  }

  @Override
  public CredentialTokenAuth getInstance() throws IOException {
    return auth;
  }

  @Override
  public void bindSingletons(BiConsumer<Object, Class<?>> binder) {
    binder.accept(manager, TokenManager.class);
  }

  @Override
  public Class<?>[] getEndpoints() {
    return new Class<?>[]{AuthEndpoint.class};
  }

  public TokenManager getManager() {
    return manager;
  }

  @Override
  public String getChangeLogPath() {
    return CHANGELOG_RESOURCE;
  }
}
