package org.aktin.broker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.aktin.broker.client.TestAdmin;
import org.aktin.broker.client.TestClient;
import org.aktin.broker.client.ClientWebsocket;
import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.ResultInfo;
import org.aktin.broker.xml.SoftwareModule;
import org.aktin.broker.xml.util.Util;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestBroker {
	private TestServer server;
	private SoftwareModule testModule = new SoftwareModule("TEST", "1");
	@Before
	public void setupServer() throws Exception{
		server = new TestServer();
		server.start_local(0);
		// TODO reset database
	}
	
	@After
	public void shutdownServer() throws Exception{
		server.stop();
		server.destroy();
	}
	@Test
	public void expectNewClientAdded() throws IOException{
		String testCn = "CN=Test Nachname,ST=Hessen,C=DE,O=DZL,OU=Uni Giessen";
		String testId = "01";
		TestClient c = new  TestClient(server.getBrokerServiceURI(), testId, testCn);
		// get status does not require authentication
		BrokerStatus s = c.getBrokerStatus();
		System.out.println(s);

		// posting status will trigger authentication
		c.postMyStatus(System.currentTimeMillis(), Collections.singletonList(testModule));

		TestAdmin a = new TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		// verify client list
		List<Node> nl = a.listNodes();
		// we should be the only node
		Assert.assertEquals(1, nl.size());
		Node node = nl.get(0);
		Assert.assertEquals(testCn, node.clientDN);
		int nodeId = node.id;
		// retrieve info only for the single node
		node = a.getNode(nodeId);
		Assert.assertEquals(testCn, node.clientDN);
		Assert.assertEquals("Test Nachname", node.getCommonName());
	}
	@Test
	public void testAddDeleteQuery() throws IOException{
		String testCn = "/CN=Test Nachname/ST=Hessen/C=DE/O=DZL/OU=Uni Giessen";
		String testId = "01";
		TestAdmin c = new  TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		// assume list is empty
		List<RequestInfo> l = c.listAllRequests();
		Assert.assertTrue(l.isEmpty());
		// add request
		// TODO use large file
		URI loc = c.createRequest("text/x-test", "test");
		System.out.println("New request: "+loc);
		l = c.listAllRequests();
		Assert.assertEquals(1, l.size());
		// try to read the query
		Reader def = c.getRequestDefinition(l.get(0).getId(),  "text/x-test");
		// delete query
		c.delete(loc);

		Assert.assertEquals("test", Util.readContent(def));
	}
	@Test
	public void reportAndReadRequestStatus() throws IOException{
		String testCn = "/CN=Test Nachname/ST=Hessen/C=DE/O=DZL/OU=Uni Giessen";
		String testId = "01";
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		TestClient c = new  TestClient(server.getBrokerServiceURI(), testId, testCn);
		// create request
		a.createRequest("text/vnd.test1", "test");
		// retrieve request
		String def = c.getMyRequestDefinitionString("0", "text/vnd.test1");
		Assert.assertEquals("test",  def);
		// report status
		c.postRequestStatus("0", RequestStatus.accepted);
		// request status
		List<RequestStatusInfo> list = a.listRequestStatus("0");
		Assert.assertEquals(1, list.size());
		Assert.assertNotNull(list.get(0).accepted);
		Assert.assertNull(list.get(0).rejected);
		Assert.assertNull(list.get(0).type); // should be no message type		
		// update status (e.g. failed)
		c.postRequestFailed("0", "Only test", new UnsupportedOperationException());
		list = a.listRequestStatus("0");
		Assert.assertNotNull(list.get(0).type); // now, there is a message	
		
	}

	@Test
	public void testAddRequestDeleteMyQuery() throws IOException{
		String testCn = "/CN=Test Nachname/ST=Hessen/C=DE/O=DZL/OU=Uni Giessen";
		String testId = "01";
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		TestClient c = new  TestClient(server.getBrokerServiceURI(), testId, testCn);
		// assume list is empty
		List<RequestInfo> l = c.listMyRequests();
		Assert.assertTrue(l.isEmpty());
		// add request
		URI loc = a.createRequest("text/x-test", "test");
		// retrieve
		c.getMyRequestDefinitionString("0", "text/x-test");
		System.out.println("New request: "+loc);
		l = c.listMyRequests();
		Assert.assertEquals(1, l.size());
		// retrieve
		c.getMyRequestDefinitionString(l.get(0).getId(), l.get(0).types[0]);
		// mark as deleted
		RequestInfo ri = l.get(0);
		Assert.assertNotNull(ri.nodeStatus);
		TestJAXRS.printXML(ri);
		c.deleteMyRequest(ri.getId());
		// delete query
		a.delete(loc);
	}
	@Test
	public void testRequestWithMultipleDefinitions() throws IOException{
		String testCn = "/CN=Test Nachname/ST=Hessen/C=DE/O=DZL/OU=Uni Giessen";
		String testId = "01";
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		Assert.assertEquals(0, a.listAllRequests().size());
		URI loc = a.createRequest("text/x-test-1", "test1");
		System.out.println("Location: "+loc);
		a.putRequestDefinition(loc, "text/x-test-2", "test2");			
		RequestInfo ri = a.getRequestInfo("0");
		List<RequestInfo> l = a.listAllRequests();
		a.delete(loc);
		l.forEach(r -> System.out.println("Request:"+r.getId()));
		Assert.assertEquals(1, l.size());
		Assert.assertEquals(ri, l.get(0));
		System.out.println("Request types: "+Arrays.toString(ri.types));
		Assert.assertEquals(2, ri.types.length);
		// check request info for single request
		Assert.assertEquals(2, ri.types.length);
	}
	@Test
	public void testRequestSubmitResult() throws IOException{
		String testCn = "/CN=Test Nachname/ST=Hessen/C=DE/O=DZL/OU=Uni Giessen";
		String testId = "01";
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		URI loc = a.createRequest("text/x-test-1", "test1");
		loc.toString();
		TestClient c = new  TestClient(server.getBrokerServiceURI(), testId, testCn);
		List<RequestInfo> l = c.listMyRequests();
		Assert.assertEquals(1, l.size());
		// submit result with aggregatorClient
		c.putRequestResult(l.get(0).getId(), "test/vnd.test.result", new ByteArrayInputStream("test-result-data".getBytes()));
		// list results with aggregatorAdmin
		List<ResultInfo> r = a.listResults(l.get(0).getId());
		a.delete(loc);
		Assert.assertTrue( c.listMyRequests().isEmpty() );
		Assert.assertEquals(1, r.size());
		Assert.assertEquals("test/vnd.test.result", r.get(0).type);
	}
	@Test
	public void testWebsocket() throws Exception{
		WebSocketClient client = new WebSocketClient();
		ClientWebsocket websocket = new ClientWebsocket();
		client.start();
		Future<Session> f = client.connect(websocket, new URI("ws://localhost:"+server.getLocalPort()+"/broker-notify"), new ClientUpgradeRequest());
		System.out.println("Connecting..");
		Session s = f.get(5, TimeUnit.SECONDS);
		s.getClass(); // don't need the session
		// connected, wait for messages
		websocket.expectedMessages.await(5, TimeUnit.SECONDS);
		client.stop();
	}
}
