package org.aktin.broker;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


import org.aktin.broker.client.AuthFilterImpl;
import org.aktin.broker.client.BrokerAdmin;
import org.aktin.broker.client.ClientWebsocket;
import org.aktin.broker.client.TestAdmin;
import org.aktin.broker.client.TestClient;
import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.NotificationListener;
import org.aktin.broker.util.AuthFilterSSLHeaders;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestWebsockets extends AbstractTestBroker{
	private static final String CLIENT_01_SERIAL = "01";
	private static final String CLIENT_01_DN = "CN=Test 1,ST=Hessen,C=DE,O=AKTIN,OU=Uni Giessen";

	private static final String CLIENT_02_SERIAL = "02";
	private static final String CLIENT_02_DN = "CN=Test 2,ST=Hessen,C=DE,O=AKTIN,OU=Uni Giessen";

	private static final String ADMIN_00_DN = "CN=Test Adm,ST=Hessen,C=DE,O=AKTIN,OU=Uni Giessen,OU=admin";
	private static final String ADMIN_00_SERIAL = "00";

	@Override
	public BrokerClient2 initializeClient(String arg) {
		AuthFilter auth;
		switch( arg ) {
		case CLIENT_01_SERIAL:
			 auth = new AuthFilterImpl(CLIENT_01_SERIAL, CLIENT_01_DN);
			 break;
		case CLIENT_02_SERIAL:
			 auth = new AuthFilterImpl(CLIENT_02_SERIAL, CLIENT_02_DN);
			 break;
		default:
			throw new IllegalArgumentException();
		}
		BrokerClient2 c = new BrokerClient2(server.getBrokerServiceURI());
		c.setAuthFilter(auth);
		return c;
	}

	@Override
	public BrokerAdmin initializeAdmin() {
		return new TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_DN);
	}

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
	
	/**
	 * Make sure that request notifications are recieved via websocket using the client library.
	 * Clients not targeted by the request should not receive notifications.
	 * @throws Exception unexpected test failure
	 */
	@Test
	public void clientRequestNotificationsOnlyForTargetedNodes() throws Exception{
		BrokerClient2 c1 = initializeClient(CLIENT_01_SERIAL);
		BrokerClient2 c2 = initializeClient(CLIENT_02_SERIAL);
		// make sure that the clients are known by the broker,
		// by contacting the broker first
		c1.listMyRequests();
		c2.listMyRequests();
		// initialize async variables
		AtomicInteger publishedId = new AtomicInteger(-1);
		AtomicInteger closedId = new AtomicInteger(-1);
		AtomicBoolean thirdPartyNotification = new AtomicBoolean(false);
		// receive expected notifications
		c1.openWebsocket(new NotificationListener() {
			@Override
			public void onResourceChanged(String resourceName) {}
			
			@Override
			public void onRequestPublished(int requestId) {
				publishedId.set(requestId);
			}
			
			@Override
			public void onRequestClosed(int requestId) {
				closedId.set(requestId);
			}
		});
		// fail with unwanted notifications of third party
		c2.openWebsocket(new NotificationListener() {
			@Override
			public void onResourceChanged(String resourceName) {
				thirdPartyNotification.set(true);
			}
			
			@Override
			public void onRequestPublished(int requestId) {
				thirdPartyNotification.set(true);
			}
			
			@Override
			public void onRequestClosed(int requestId) {
				thirdPartyNotification.set(true);
			}
		});
		
		BrokerAdmin a = initializeAdmin();
		int qid = a.createRequest("text/x-test-1", "test1");
		Thread.sleep(300);
		// make sure the publication was not sent yet
		Assert.assertEquals(-1, publishedId.get());
		Assert.assertFalse(thirdPartyNotification.get());
		// make sure selected node is c1
		int selectedNode = 0;
		Assert.assertEquals(CLIENT_01_DN, a.getNode(selectedNode).clientDN);
		// limit request to only c1
		a.setRequestTargetNodes(qid, new int[] {selectedNode});
		// publish request
		a.publishRequest(qid);
		Thread.sleep(300);
		// make sure the publication was broadcast
		Assert.assertEquals(qid, publishedId.get());
		Assert.assertEquals(-1, closedId.get());
		// same with closed id
		a.closeRequest(qid);
		Thread.sleep(300);
		Assert.assertEquals(qid, closedId.get());
		// make sure that other nodes did not get any notification if the request was not targeted for them
		Assert.assertFalse(thirdPartyNotification.get());
		
	}

	/**
	 * Test basic websocket functionality without using the broker-client libraries
	 * @throws Exception unexpected test failure
	 */
	@Test
	public void testWebsocket() throws Exception{
		WebSocketClient client = new WebSocketClient();
		ClientWebsocket websocket = new ClientWebsocket();
		ClientUpgradeRequest req = new ClientUpgradeRequest();
		// set authentication headers for socket handshake
		TestClient.setAuthenticatedHeaders(req::setHeader, CLIENT_01_SERIAL, CLIENT_01_DN);
		websocket.prepareForMessages(1);
		client.start();
		Future<Session> f = client.connect(websocket, new URI("ws://localhost:"+server.getLocalPort()+"/broker/my/websocket"), req);
		System.out.println("Connecting..");
		Session s = f.get(5, TimeUnit.SECONDS);
		s.getClass(); // don't need the session
		// connected, wait for messages
		websocket.waitForMessages(5, TimeUnit.SECONDS);
		Assert.assertEquals("Welcome message expected from after websocket connect", 1, websocket.messages.size());


		// add request
		BrokerAdmin a = initializeAdmin();
		int qid = a.createRequest("text/x-test-1", "test1");
		// expect notification for published request
		websocket.messages.clear();
		websocket.prepareForMessages(1);
		// publish request
		a.publishRequest(qid);
		// wait for notification
		websocket.waitForMessages(5, TimeUnit.SECONDS);
		Assert.assertEquals("request notification expected", 1, websocket.messages.size());
		Assert.assertEquals("published 0", websocket.messages.get(0));

		// expect notification for closed request
		websocket.messages.clear();
		websocket.prepareForMessages(1);
		// close request
		a.closeRequest(qid);
		// wait for notification
		websocket.waitForMessages(5, TimeUnit.SECONDS);
		Assert.assertEquals("request notification expected", 1, websocket.messages.size());
		Assert.assertEquals("closed 0", websocket.messages.get(0));

		// terminate websocket client
		client.stop();
	}

}
