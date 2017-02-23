package org.aktin.broker;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.time.Instant;
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
import org.aktin.broker.xml.util.Util;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestBroker {
	private static final String CLIENT_01_CN = "CN=Test 1,ST=Hessen,C=DE,O=AKTIN,OU=Uni Giessen";
	private static final String CLIENT_02_CN = "CN=Test 2,ST=Hessen,C=DE,O=AKTIN,OU=Uni Giessen";
	private static final String ADMIN_00_CN = "CN=Test Adm,ST=Hessen,C=DE,O=AKTIN,OU=Uni Giessen,OU=admin";
	private static final String CLIENT_01_SERIAL = "01";
	private static final String CLIENT_02_SERIAL = "02";
	private static final String ADMIN_00_SERIAL = "00";
	private TestServer server;

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
	public void expectClientModulesInDatabase() throws IOException{
		// TODO test update of software modules: overwrite modules, read back modules
		String testCn = "CN=Test Nachname,ST=Hessen,C=DE,O=DZL,OU=Uni Giessen";
		String testId = "01";
		TestClient c = new  TestClient(server.getBrokerServiceURI(), testId, testCn);
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		c.postMyStatus(System.currentTimeMillis(), Collections.singletonMap("TEST", "test1"));
		Assert.assertEquals("test1", a.getNode(0).modules.get("TEST"));
		c.postMyStatus(System.currentTimeMillis(), Collections.singletonMap("TEST", "test2"), new XmlPayload("elval","atval"));
		Assert.assertEquals("test2", a.getNode(0).modules.get("TEST"));
		// TODO should be only TEST -> test2 in database
		Reader r = a.getNodeStatus(0);
		assertNotNull(r);
		System.out.println(Util.readContent(r));
		r.close();
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
		c.postMyStatus(System.currentTimeMillis(), Collections.singletonMap("TEST", "1"), new XmlPayload("el-val", "attr-val") );

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
//		TestJAXRS.printXML(node);
		Assert.assertEquals(testCn, node.clientDN);
		Assert.assertEquals("Test Nachname", node.getCommonName());
	}
	@Test
	public void testAddDeleteQuery() throws IOException{
		String testCn = "CN=Test Nachname,ST=Hessen,C=DE,O=DZL,OU=Uni Giessen,OU=admin";
		String testId = "01";
		TestAdmin c = new  TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		// assume list is empty
		List<RequestInfo> l = c.listAllRequests();
		Assert.assertTrue(l.isEmpty());
		// add request
		// TODO use large file
		String qid = c.createRequest("text/x-test", "test");
		System.out.println("New request: "+qid);
		l = c.listAllRequests();
		Assert.assertEquals(1, l.size());
		// try to read the query
		Reader def = c.getRequestDefinition(l.get(0).getId(),  "text/x-test");
		// delete query
		c.deleteRequest(qid);

		Assert.assertEquals("test", Util.readContent(def));
	}

	@Test
	public void expect404ForNonExistentRequests() throws IOException{
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_CN);
		assertNull(a.getRequestDefinition("0", "text/plain"));
		TestClient c = new  TestClient(server.getBrokerServiceURI(), CLIENT_01_SERIAL, CLIENT_01_CN);
		assertNull(c.getMyRequestDefinitionString("0", "text/plain"));
		assertNull(c.getMyRequestDefinitionXml("0", "text/plain"));
		assertNull(c.getMyRequestDefinitionLines("0", "text/plain"));
	}
	@Test
	public void reportAndReadRequestStatus() throws IOException{
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_CN);
		TestClient c = new  TestClient(server.getBrokerServiceURI(), CLIENT_01_SERIAL, CLIENT_01_CN);
		// create request
		String rid = a.createRequest("text/vnd.test1", "test");
		a.publishRequest(rid);
		// listing requests should not update the request status for the nodes
		List<RequestInfo> ril = c.listMyRequests();
		assertEquals(1, ril.size());
		List<RequestStatusInfo> list = a.listRequestStatus(rid);
		// no status info expected
		assertEquals(0, list.size());
		
		// retrieve request
		String def = c.getMyRequestDefinitionString("0", "text/vnd.test1");
		assertEquals("test",  def);
		// request status should be automatically set to retrieved
		list = a.listRequestStatus("0");
		assertEquals(1, list.size());
		assertNotNull(list.get(0).retrieved);

		// report status
		c.postRequestStatus("0", RequestStatus.queued);
		// request status
		list = a.listRequestStatus("0");
		assertEquals(1, list.size());
		assertNotNull(list.get(0).queued);
		assertTrue(list.get(0).queued.isAfter(list.get(0).retrieved));
		assertNull(list.get(0).rejected);
		assertNull(list.get(0).type); // should be no message type		
		// update status (e.g. failed)
		c.postRequestFailed("0", "Only test", new UnsupportedOperationException());
		list = a.listRequestStatus("0");
		// now, there is a message
		Assert.assertEquals("text/plain", list.get(0).type);
		
	}

	@Test
	public void testAddRequestDeleteMyQuery() throws IOException{
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_CN);
		TestClient c = new  TestClient(server.getBrokerServiceURI(), CLIENT_01_SERIAL, CLIENT_01_CN);
		// assume list is empty
		List<RequestInfo> l = c.listMyRequests();
		Assert.assertTrue(l.isEmpty());
		// add request
		String qid = a.createRequest("text/x-test", "test");
		a.publishRequest(qid);
		
		// retrieve
		c.getMyRequestDefinitionString("0", "text/x-test");
		System.out.println("New request: "+qid);
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
		a.deleteRequest(qid);
	}
	@Test
	public void testRequestWithMultipleDefinitions() throws IOException{
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_CN);
		Assert.assertEquals(0, a.listAllRequests().size());
		String qid = a.createRequest("text/x-test-1", "test1");
		a.putRequestDefinition(qid, "text/x-test-2", "test2");			
		RequestInfo ri = a.getRequestInfo("0");
		List<RequestInfo> l = a.listAllRequests();
		a.deleteRequest(qid); // ????
		l.forEach(r -> System.out.println("Request:"+r.getId()));
		Assert.assertEquals(1, l.size());
		Assert.assertEquals(ri, l.get(0));
		System.out.println("Request types: "+Arrays.toString(ri.types));
		Assert.assertEquals(2, ri.types.length);
		// check request info for single request
		Assert.assertEquals(2, ri.types.length);
		// replace definition
		a.putRequestDefinition(qid, "text/x-test-2", "test2-2");
		Reader r = a.getRequestDefinition(qid, "text/x-test-2");
		Assert.assertEquals("test2-2", Util.readContent(r));		
	}
	@Test
	public void failQueryVerifyErrorMessage() throws IOException{
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_CN);
		TestClient c = new  TestClient(server.getBrokerServiceURI(), CLIENT_01_SERIAL, CLIENT_01_CN);
		// assume list is empty
		List<RequestInfo> l = c.listMyRequests();
		Assert.assertTrue(l.isEmpty());
		// add request
		String qid = a.createRequest("text/x-test", "test");
		a.publishRequest(qid);
		
		// report failure
		c.getMyRequestDefinitionString("0", "text/x-test");
		c.postRequestFailed("0", "message", new AssertionError());

		// retrieve failure
		try( BufferedReader b = new BufferedReader(a.getRequestNodeMessage(qid, 0)) ){
			Assert.assertEquals(b.readLine(), "message");
			Assert.assertEquals(AssertionError.class.getName(), b.readLine());
		}
		// TODO unit test for null request node message
	}
	@Test
	public void targetedRequestsInvisibleToOtherNodes() throws IOException{
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_CN);
		TestClient c1 = new  TestClient(server.getBrokerServiceURI(), CLIENT_01_SERIAL, CLIENT_01_CN);
		TestClient c2 = new  TestClient(server.getBrokerServiceURI(), CLIENT_02_SERIAL, CLIENT_02_CN);
		// assume list is empty
		assertEquals(0, c1.listMyRequests().size());
		assertEquals(0, c2.listMyRequests().size());
		// make sure we have the correct node id
		assertEquals(1, c2.getMyNode().id);

		// create request and limit to second node
		String qid = a.createRequest("text/x-test-1", "test1");
		a.setRequestTargetNodes(qid, new int[]{1});
		int[] nodes = a.getRequestTargetNodes(qid);
		assertEquals(1, nodes.length);
		assertEquals(1, nodes[0]);
		a.publishRequest(qid);

		// assume list is still empty
		assertEquals(0, c1.listMyRequests().size());
		assertEquals(1, c2.listMyRequests().size());

		// clear targets
		a.clearRequestTargetNodes(qid);
		// should now be visible to all nodes
		assertEquals(1, c1.listMyRequests().size());
		assertEquals(1, c2.listMyRequests().size());
	}

	@Test
	public void testRequestSubmitResult() throws IOException{
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_CN);
		String qid = a.createRequest("text/x-test-1", "test1");
		a.publishRequest(qid);

		TestClient c = new  TestClient(server.getBrokerServiceURI(), CLIENT_01_SERIAL, CLIENT_01_CN);
		List<RequestInfo> l = c.listMyRequests();
		Assert.assertEquals(1, l.size());
		// submit result with aggregatorClient
		c.putRequestResult(l.get(0).getId(), "test/vnd.test.result", new ByteArrayInputStream("test-result-data".getBytes()));
		// list results with aggregatorAdmin
		List<ResultInfo> r = a.listResults(l.get(0).getId());
		a.deleteRequest(qid);
		Assert.assertTrue( c.listMyRequests().isEmpty() );
		Assert.assertEquals(1, r.size());
		Assert.assertEquals("test/vnd.test.result", r.get(0).type);
	}
	@Test
	public void verifyLastContactUpdated() throws IOException{
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), ADMIN_00_SERIAL, ADMIN_00_CN);
		String qid = a.createRequest("text/x-test-1", "test1");
		a.publishRequest(qid);

		TestClient c = new  TestClient(server.getBrokerServiceURI(), CLIENT_01_SERIAL, CLIENT_01_CN);
		List<RequestInfo> l = c.listMyRequests();
		Assert.assertEquals(1, l.size());
		// we should have a timestamp for last contact of the client
		Node node = a.getNode(0);
		Assert.assertEquals(CLIENT_01_CN, node.clientDN);
		Assert.assertNotNull(node);
		Instant t1 = node.lastContact;
		Assert.assertNotNull(t1);
		// do any *authenticated* interaction with the server and check whether the timestamp was updated
		c.listMyRequests();
		node = a.getNode(0);
		Assert.assertTrue(t1.isBefore(node.lastContact));
	}
	@Test
	public void testWebsocket() throws Exception{
		WebSocketClient client = new WebSocketClient();
		ClientWebsocket websocket = new ClientWebsocket();
		websocket.prepareForMessages(1);
		client.start();
		Future<Session> f = client.connect(websocket, new URI("ws://localhost:"+server.getLocalPort()+"/broker-notify"), new ClientUpgradeRequest());
		System.out.println("Connecting..");
		Session s = f.get(5, TimeUnit.SECONDS);
		s.getClass(); // don't need the session
		// connected, wait for messages
		websocket.waitForMessages(5, TimeUnit.SECONDS);
		Assert.assertEquals("Welcome message expected from after websocket connect", 1, websocket.messages.size());


		// add request
		String testCn = "CN=Test Nachname,ST=Hessen,C=DE,O=AKTIN,OU=Uni Giessen,OU=admin";
		String testId = "01";
		TestAdmin a = new  TestAdmin(server.getBrokerServiceURI(), testId, testCn);
		String qid = a.createRequest("text/x-test-1", "test1");
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
