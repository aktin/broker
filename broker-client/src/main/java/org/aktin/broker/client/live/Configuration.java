package org.aktin.broker.client.live;

import java.net.URI;

import org.aktin.broker.client2.AuthFilter;

public interface Configuration {
	// broker config
	URI getBrokerEndpointURI();
	String getRequestMediatype();
	String getResultMediatype();
	// TODO authentication
	String getAuthClass();
	String getAuthParam();

}
