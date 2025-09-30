package org.aktin.broker.auth.otp;

import java.util.function.BiConsumer;
import org.aktin.broker.auth.otp.controller.AuthEndpoint;
import org.aktin.broker.auth.otp.controller.UserEndpoint;
import org.aktin.broker.auth.otp.repository.FsUserRepository;
import org.aktin.broker.auth.otp.repository.UserRepository;
import org.aktin.broker.auth.otp.service.FsUserService;
import org.aktin.broker.auth.otp.service.UserAuthService;
import org.aktin.broker.auth.otp.service.UserAuthServiceImpl;
import org.aktin.broker.auth.otp.service.UserService;
import org.aktin.broker.auth.otp.token.CredentialTokenAuth;
import org.aktin.broker.auth.otp.token.TokenManager;
import org.aktin.broker.auth.otp.token.TokenManagerImpl;
import org.aktin.broker.auth.otp.utils.OtpProvider;
import org.aktin.broker.auth.otp.utils.PasswordHasher;
import org.aktin.broker.auth.otp.utils.Pbkdf2PasswordHasher;
import org.aktin.broker.auth.otp.utils.YubicoOtpProvider;
import org.aktin.broker.server.auth.AbstractAuthProvider;

/**
 * The main provider that bootstraps and configures the credential/token authentication module.
 * <p>
 * This class implements {@link org.aktin.broker.server.auth.AbstractAuthProvider} to integrate with the AKTIN Broker's lifecycle. It is the central point where all services, repositories, and
 * endpoints for this authentication mechanism are instantiated and wired together.
 */
public class CredentialTokenAuthProvider extends AbstractAuthProvider {

  private final TokenManager manager;
  private final CredentialTokenAuth tokenAuth;
  private final UserRepository repository;
  private final UserService service;
  private final UserAuthService userAuth;

  public CredentialTokenAuthProvider() {
    this.manager = new TokenManagerImpl();
    this.tokenAuth = new CredentialTokenAuth(manager);
    this.repository = new FsUserRepository();
    PasswordHasher hasher = new Pbkdf2PasswordHasher();
    OtpProvider otpProvider = new YubicoOtpProvider();
    this.service = new FsUserService(repository, hasher, otpProvider);
    this.userAuth = new UserAuthServiceImpl(service);
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
    binder.accept(repository, UserRepository.class);
    binder.accept(service, UserService.class);
    binder.accept(userAuth, UserAuthService.class);
  }

  @Override
  public Class<?>[] getEndpoints() {
    return new Class<?>[]{AuthEndpoint.class, UserEndpoint.class};
  }
}
