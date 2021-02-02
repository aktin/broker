package org.aktin.broker;

import org.aktin.broker.auth.AuthenticationRequestFilter;
import org.aktin.broker.auth.AuthorizationRequestFilter;
import org.aktin.broker.rest.AggregatorEndpoint;
import org.aktin.broker.rest.BrokerStatusEndpoint;
import org.aktin.broker.rest.DownloadEndpoint;
import org.aktin.broker.rest.ExportEndpoint;
import org.aktin.broker.rest.MyBrokerEndpoint;
import org.aktin.broker.rest.NodeInfoEndpoint;
import org.aktin.broker.rest.RequestAdminEndpoint;
import org.aktin.broker.websocket.MyBrokerWebsocket;
import org.aktin.broker.websocket.RequestAdminWebsocket;

public class Broker {
	public static final String SERVICE_URL = "/broker/";
	public static final Class<?>[] ENDPOINTS = new Class<?>[]{
		BrokerStatusEndpoint.class,
		MyBrokerEndpoint.class,
		RequestAdminEndpoint.class,
		NodeInfoEndpoint.class,
		AggregatorEndpoint.class,
		ExportEndpoint.class,
		DownloadEndpoint.class
	};
	public static final Class<?>[] WEBSOCKETS = new Class<?>[]{
		MyBrokerWebsocket.class,
		RequestAdminWebsocket.class,
	};
	public static final Class<?>[] AUTH_FILTERS = new Class<?>[]{
		AuthenticationRequestFilter.class,
		AuthorizationRequestFilter.class,
	};
}
