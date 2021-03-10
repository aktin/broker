package org.aktin.broker.client.live;


public interface ClientConfiguration extends BrokerConfiguration{
	// broker config
	String getRequestMediatype();
	String getResultMediatype();

}
