package org.aktin.broker.auth.cred2;

import java.io.IOException;
import java.util.function.BiConsumer;
import org.aktin.broker.auth.cred2.auth.CredentialTokenAuth;
import org.aktin.broker.auth.cred2.auth.TokenManager;
import org.aktin.broker.auth.cred2.auth.TokenManagerImpl;
import org.aktin.broker.auth.cred2.http.AuthEndpoint;
import org.aktin.broker.auth.cred2.http.UserEndpoint;
import org.aktin.broker.auth.cred2.repository.JdbcUserRepository;
import org.aktin.broker.auth.cred2.repository.UserRepository;
import org.aktin.broker.auth.cred2.service.JdbcUserService;
import org.aktin.broker.auth.cred2.service.UserAuthService;
import org.aktin.broker.auth.cred2.service.UserAuthServiceImpl;
import org.aktin.broker.auth.cred2.service.UserService;
import org.aktin.broker.auth.cred2.utils.PasswordHasher;
import org.aktin.broker.auth.cred2.utils.Pbkdf2PasswordHasher;
import org.aktin.broker.auth.cred2.utils.TokenPruneService;
import org.aktin.broker.server.auth.AbstractAuthProvider;
import org.aktin.broker.server.auth.DatabaseChangelogProvider;

public class CredentialTokenAuthProvider extends AbstractAuthProvider implements DatabaseChangelogProvider {

  public static final String CHANGELOG_RESOURCE = "userCreds.xml";

  private final TokenManager manager;
  private final TokenPruneService pruner;
  private final CredentialTokenAuth tokenAuth;
  private final UserRepository repository;
  private final PasswordHasher hasher;
  private final UserService service;
  private final UserAuthService userAuth;

  public CredentialTokenAuthProvider() {
    this.manager = new TokenManagerImpl();
    this.pruner = new TokenPruneService(this.manager).start();
    this.tokenAuth = new CredentialTokenAuth(manager);
    this.repository = new JdbcUserRepository();
    this.hasher = new Pbkdf2PasswordHasher();
    this.service = new JdbcUserService(repository, hasher);
    this.userAuth = new UserAuthServiceImpl(service, hasher);
  }

  @Override
  public CredentialTokenAuth getInstance() {
    return tokenAuth;
  }

  public TokenManager getManager() {
    return manager;
  }

  @Override
  public void bindSingletons(BiConsumer<Object, Class<?>> binder) {
    binder.accept(manager, TokenManager.class);
    binder.accept(pruner, AutoCloseable.class);
    binder.accept(repository, UserRepository.class);
    binder.accept(hasher, PasswordHasher.class);
    binder.accept(service, UserService.class);
    binder.accept(userAuth, UserAuthService.class);
  }

  @Override
  public Class<?>[] getEndpoints() {
    return new Class<?>[]{AuthEndpoint.class, UserEndpoint.class};
  }

  @Override
  public String getChangeLogPath() {
    return CHANGELOG_RESOURCE;
  }
}
