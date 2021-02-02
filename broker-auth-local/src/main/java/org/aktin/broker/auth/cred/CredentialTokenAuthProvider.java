package org.aktin.broker.auth.cred;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.aktin.broker.server.auth.AbstractAuthProviderFactory;

public class CredentialTokenAuthProvider extends AbstractAuthProviderFactory{
	private TokenManager manager;

	@Override
	public CredentialTokenAuth getInstance() throws IOException {
		// initialize token manager
		manager = new TokenManager();
		// return 
		return new CredentialTokenAuth(manager);
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
