package org.aktin.broker;

import org.aktin.broker.client.BrokerAdmin;
import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.util.AuthFilterSSLHeaders;
import org.junit.After;
import org.junit.Before;

public abstract class AbstractTestBroker {
	protected BrokerTestServer server;
	protected BrokerClient client;
	protected BrokerAdmin admin;

	@Before
	public void setupServer() throws Exception{
		server = new BrokerTestServer(new AuthFilterSSLHeaders());
		server.start_local(0);
		// TODO reset database
	}
	
	@After
	public void shutdownServer() throws Exception{
		server.stop();
		server.destroy();
	}

	public abstract BrokerClient initializeClient(String arg);
	
	public abstract BrokerAdmin initializeAdmin();
}
