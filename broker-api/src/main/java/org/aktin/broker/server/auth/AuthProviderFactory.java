package org.aktin.broker.server.auth;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public interface AuthProviderFactory {

	public void setBasePath(Path path);
	/**
	 * Get the instance to be used for authentication. This method will usually be called only once.
	 * Nevertheless, implementers should make sure, that the method always returns the same instance.
	 * @return authentication singleton instance
	 * @throws IOException IO error during initialization
	 */
	public HeaderAuthentication getInstance() throws IOException;

	/**
	 * Bind additional singletons to custom interfaces needed by the endpoints
	 * returned by {@link #getEndpoints()}.
	 * @param binder binds singleton instances to classes (to be injected into the endpoints) 
	 */
	default void bindSingletons(BiConsumer<Object,Class<?>> binder) {};
	/**
	 * Return additional JAXRS endpoints needed for the authentication.
	 * @return array of endpoint classes to be registered with JAXRS
	 */
	default Class<?>[] getEndpoints(){return new Class<?>[] {};};

}
