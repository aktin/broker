package org.aktin.broker.admin.auth.cred;

import java.io.IOException;
import java.util.function.BiConsumer;

import org.aktin.broker.server.auth.AbstractAuthProviderFactory;
import org.aktin.broker.server.auth.HeaderAuthentication;

public class CredentialTokenAuthProvider extends AbstractAuthProviderFactory{
	private TokenManager manager;

	@Override
	public HeaderAuthentication getInstance() throws IOException {
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

}
