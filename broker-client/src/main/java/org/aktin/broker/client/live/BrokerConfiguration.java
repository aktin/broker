package org.aktin.broker.client.live;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.aktin.broker.client2.AuthFilter;

public interface BrokerConfiguration {
	URI getBrokerEndpointURI();
	String getAuthClass();
	String getAuthParam();

	default AuthFilter instantiateAuthFilter() throws IllegalArgumentException {
		try {
			Class<?> clazz = Class.forName(getAuthClass());
			Constructor<? extends AuthFilter> constructor 
				= clazz.asSubclass(AuthFilter.class).getConstructor(String.class);
			return constructor.newInstance(getAuthParam());
		} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException e) {
			throw new IllegalArgumentException("Unable to instantiate AuthFilter for class "+getAuthClass(), e);
		}
	}

}
