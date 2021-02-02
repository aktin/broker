package org.aktin.broker.auth.cred;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.aktin.broker.server.auth.AbstractAuthProviderFactory;

public class CredentialTokenAuthProvider extends AbstractAuthProviderFactory{
	private TokenManager manager;
	private CredentialTokenAuth auth;

	public CredentialTokenAuthProvider(String simplePassword) {
		this.manager = new TokenManager(simplePassword);
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
		return new Class<?>[] { AuthEndpoint.class };
	}

	public TokenManager getManager() {
		return manager;
	}
}
