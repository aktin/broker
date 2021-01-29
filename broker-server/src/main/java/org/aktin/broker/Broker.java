package org.aktin.broker;

import org.aktin.broker.notify.MyBrokerWebsocket;
import org.aktin.broker.notify.RequestAdminWebsocket;
import org.aktin.broker.rest.AggregatorEndpoint;
import org.aktin.broker.rest.BrokerStatusEndpoint;
import org.aktin.broker.rest.DownloadEndpoint;
import org.aktin.broker.rest.ExportEndpoint;
import org.aktin.broker.rest.MyBrokerEndpoint;
import org.aktin.broker.rest.NodeInfoEndpoint;
import org.aktin.broker.rest.RequestAdminEndpoint;

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
}
