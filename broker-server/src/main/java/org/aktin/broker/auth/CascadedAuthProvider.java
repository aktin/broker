package org.aktin.broker.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import org.aktin.broker.server.auth.AbstractAuthProviderFactory;
import org.aktin.broker.server.auth.AuthProviderFactory;
import org.aktin.broker.server.auth.HeaderAuthentication;

public class CascadedAuthProvider extends AbstractAuthProviderFactory{

	private List<AuthProviderFactory> providers;

	public CascadedAuthProvider(List<AuthProviderFactory> providers) {
		this.providers = providers;
	}
	@Override
	public HeaderAuthentication getInstance() throws IOException {
		HeaderAuthentication[] inst = new HeaderAuthentication[providers.size()];
		for( int i=0; i<providers.size(); i++ ) {
			inst[i] = providers.get(i).getInstance();
		}
		return new CascadedHeaderAuthentication(inst);
	}
	@Override
	public void bindSingletons(BiConsumer<Object, Class<?>> binder) {
		for( AuthProviderFactory delegate : providers ) {
			delegate.bindSingletons(binder);
		}
	}
	@Override
	public Class<?>[] getEndpoints() {
		final ArrayList<Class<?>> endpoints = new ArrayList<Class<?>>();
		providers.forEach( p -> endpoints.addAll(Arrays.asList(p.getEndpoints())) );
		return endpoints.toArray(new Class<?>[endpoints.size()]);
	}

}
