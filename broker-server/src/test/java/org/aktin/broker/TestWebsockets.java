package org.aktin.broker;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.aktin.broker.client.AuthFilterImpl;
import org.aktin.broker.client.BrokerAdmin;
import org.aktin.broker.client.ClientWebsocket;
import org.aktin.broker.client.TestAdmin;
import org.aktin.broker.client.TestClient;
import org.aktin.broker.client2.AdminNotificationListener;
import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.client2.BrokerAdmin2;
import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.NotificationListener;
import org.aktin.broker.util.AuthFilterSSLHeaders;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatus;
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
	public BrokerAdmin2 initializeAdmin() {
		BrokerAdmin2 a = new BrokerAdmin2(server.getBrokerServiceURI());
		a.setAuthFilter(new AuthFilterImpl(ADMIN_00_SERIAL, ADMIN_00_DN));
		return a;
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

	private static final void sleepForWebsocketAction() {
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * Make sure that request notifications are recieved via websocket using the client library.
	 * Clients not targeted by the request should not receive notifications.
	 * @throws IOException unexpected test failure
	 */
	@Test
	public void clientRequestNotificationsOnlyForTargetedNodes() throws IOException {
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
		sleepForWebsocketAction();
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
		sleepForWebsocketAction();
		// make sure the publication was broadcast
		Assert.assertEquals(qid, publishedId.get());
		Assert.assertEquals(-1, closedId.get());
		// same with closed id
		a.closeRequest(qid);
		sleepForWebsocketAction();
		Assert.assertEquals(qid, closedId.get());
		// make sure that other nodes did not get any notification if the request was not targeted for them
		Assert.assertFalse(thirdPartyNotification.get());
		
	}

	@Test
	public void adminNotificationsForAllClientActions() throws IOException{
		BrokerAdmin2 a = initializeAdmin();

		// initialize async variables
		AtomicReference<String> action = new AtomicReference<>();
		AtomicReference<List<Object>> args = new AtomicReference<>();

		// open websocket
		a.openWebsocket(new AdminNotificationListener() {
			@Override
			public void onResourceUpdate(int nodeId, String resourceId) {
				action.set("resource");
				args.set(Arrays.asList(nodeId, resourceId));
			}
			
			@Override
			public void onRequestStatusUpdate(int requestId, int nodeId, String status) {
				action.set("status");
				args.set(Arrays.asList(requestId, nodeId, RequestStatus.valueOf(status)));
			}
			
			@Override
			public void onRequestResultUpdate(int requestId, int nodeId, String mediaType) {
				action.set("result");
				args.set(Arrays.asList(requestId, nodeId, mediaType));
			}
			
			@Override
			public void onRequestPublished(int requestId) {
				action.set("published");
				args.set(Collections.singletonList(requestId));
			}
			
			@Override
			public void onRequestCreated(int requestId) {
				action.set("created");
				args.set(Collections.singletonList(requestId));
			}
			
			@Override
			public void onRequestClosed(int requestId) {
				action.set("closed");
				args.set(Collections.singletonList(requestId));
			}
		});

		BrokerClient2 c1 = initializeClient(CLIENT_01_SERIAL);
		BrokerClient2 c2 = initializeClient(CLIENT_02_SERIAL);
		// make sure that the clients are known by the broker,
		// by contacting the broker first
		c1.listMyRequests();
		c2.listMyRequests();
		
		// create request
		int rid = a.createRequest();
		sleepForWebsocketAction();
		Assert.assertEquals("created", action.get());
		Assert.assertEquals(rid, args.get().get(0));
		
		// add request definitions
		a.putRequestDefinition(rid, "application/xml", "<xml/>");
		a.putRequestDefinition(rid, "application/json", "{}");
		sleepForWebsocketAction();
		// make sure there were no other notifications for adding request definitions
		Assert.assertEquals("created", action.get());

		// publish request
		a.publishRequest(rid);
		sleepForWebsocketAction();
		Assert.assertEquals("published", action.get());
		Assert.assertEquals(rid, args.get().get(0));

		// fetch requests
		List<RequestInfo> l = c2.listMyRequests();
		Assert.assertEquals(1, l.size());
		l = c1.listMyRequests();
		Assert.assertEquals(1, l.size());
		RequestInfo ri = l.get(0);
		
		// listing should not generate a notification
		sleepForWebsocketAction();
		Assert.assertEquals("published", action.get());
		// post retrieved status for node 0
		c1.postRequestStatus(ri.getId(), RequestStatus.retrieved);
		sleepForWebsocketAction();
		Assert.assertEquals("status", action.get());
		Assert.assertEquals(rid, args.get().get(0));
		Assert.assertEquals(0, args.get().get(1)); // should be node 0
		Assert.assertEquals(RequestStatus.retrieved, args.get().get(2)); // should be node 0
		// no need to test all statuses. they are processed on the server in the same method

		// post failed status for node 1
		c2.postRequestFailed(ri.getId(), "failed", new RuntimeException("bla"));
		sleepForWebsocketAction();
		Assert.assertEquals("status", action.get());
		Assert.assertEquals(rid, args.get().get(0));
		Assert.assertEquals(1, args.get().get(1)); // should be node 1
		Assert.assertEquals(RequestStatus.failed, args.get().get(2)); // should be node 0
		
		// post result
		c1.putRequestResult(ri.getId(), "text/plain+result", "result-content");
		sleepForWebsocketAction();
		Assert.assertEquals("result", action.get());
		Assert.assertEquals(rid, args.get().get(0));
		Assert.assertEquals(0, args.get().get(1)); // should be node 1
		Assert.assertEquals("text/plain+result", args.get().get(2)); // posted media type

		// close result
		a.closeRequest(rid);
		sleepForWebsocketAction();
		Assert.assertEquals("closed", action.get());
		Assert.assertEquals(rid, args.get().get(0));

		
		// verify resource updates
		c1.putMyResource("bla", "application/json", "{}");
		sleepForWebsocketAction();
		Assert.assertEquals("resource", action.get());
		Assert.assertEquals(0, args.get().get(0)); // should be node 0
		Assert.assertEquals("bla", args.get().get(1)); // should be node 0
		
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
