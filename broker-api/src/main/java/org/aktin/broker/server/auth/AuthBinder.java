package org.aktin.broker.server.auth;

/**
 * Helper interface to bind additional singletons to their correspondings classes
 * @author R.W.Majeed
 *
 */
public interface AuthBinder {

	<T extends Object> void  bind(T instance, Class<? super T> contract);
	
}
