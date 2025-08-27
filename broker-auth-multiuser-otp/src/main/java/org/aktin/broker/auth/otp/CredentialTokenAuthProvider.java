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
import org.aktin.broker.auth.otp.utils.OtpVerificationService;
import org.aktin.broker.auth.otp.utils.PasswordHasher;
import org.aktin.broker.auth.otp.utils.Pbkdf2PasswordHasher;
import org.aktin.broker.auth.otp.utils.YubicoVerificationService;
import org.aktin.broker.server.auth.AbstractAuthProvider;

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
    this.service = new FsUserService(repository, hasher);
    OtpVerificationService otpVerification = new YubicoVerificationService();
    this.userAuth = new UserAuthServiceImpl(service, otpVerification);
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
