package org.aktin.broker;

public class Broker {
	public static final String SERVICE_URL = "/broker/";
	public static final Class<?>[] ENDPOINTS = new Class<?>[]{
		BrokerStatusEndpoint.class,
		MyBrokerEndpoint.class,
		RequestAdminEndpoint.class,
		NodeInfoEndpoint.class,
		AggregatorEndpoint.class
	};
}
