package org.aktin.broker.auth.cred;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.aktin.broker.server.auth.AbstractAuthProvider;

public class CredentialTokenAuthProvider extends AbstractAuthProvider{
	private TokenManager manager;
	private CredentialTokenAuth auth;

	/**
	 * Specific constructor to use a single simple password
	 * @param simplePassword password
	 */
	public CredentialTokenAuthProvider(String simplePassword) {
		this.manager = new TokenManager(simplePassword);
		this.auth = new CredentialTokenAuth(manager);
	}

	/**
	 * Default constructor using random password
	 */
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
		return new Class<?>[] { AuthEndpoint.class };
	}

	public TokenManager getManager() {
		return manager;
	}
}
