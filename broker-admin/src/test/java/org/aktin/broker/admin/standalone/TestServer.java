package org.aktin.broker.admin.standalone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;

import org.aktin.broker.client.BrokerAdmin;
import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.broker.xml.RequestStatus;

public class TestServer implements Configuration{
	private HttpServer http;
	
	@Override
	public InputStream readAPIKeyProperties() {
		return getClass().getResourceAsStream("/api-keys.properties");
	}

	@Override
	public String getDatabasePath() {
		return "target/broker";
	}

	@Override
	public String getAggregatorDataPath() {
		return "target/aggregator-data";
	}
	@Override
	public String getBrokerDataPath() {
		return "target/broker-data";
	}

	@Override
	public String getTempDownloadPath() {
		return "target/download-temp";
	}

	@Override
	public int getPort() {
		return 8080;
	}

	/**
	 * Starts the broker admin interface for debugging (e.g. from Eclipse)
	 * To use a constant password, specify system property: aktin.broker.password
	 *
	 * @param args command line arguments: port can be specified optionally
	 * @throws Exception any error
	 */
	public static void main(String[] args) throws Exception{
		// use port if specified
		int port;
		if( args.length == 0 ){
			port = 8080;
		}else if( args.length == 1 ){
			port = Integer.parseInt(args[0]);
		}else{
			System.err.println("Too many command line arguments!");
			System.err.println("Usage: "+HttpServer.class.getCanonicalName()+" [port]");
			System.exit(-1);
			return;
		}
		// define password for test instance
		System.setProperty("aktin.broker.password", "test");

		
		// load hsql driver
		Class.forName("org.hsqldb.jdbcDriver");
		
		// start server
		TestServer server = new TestServer();
		HttpServer http = new HttpServer(server);
		try{
			http.start(new InetSocketAddress(port));
			server.http = http;
			System.err.println("Broker service at: "+http.getBrokerServiceURI());
			

			server.addDemoRequests();
			server.simulateNodes();
			http.join();
		}finally{
			http.destroy();
		}
	}

	private URI getAdminBaseURI() {
		return http.getBrokerServiceURI().resolve("../admin");
	}

	private String retrieveAdminAuthToken() throws IOException {
		HttpURLConnection c = (HttpURLConnection)getAdminBaseURI().resolve("auth/login").toURL().openConnection();
		c.setRequestMethod("POST");
		c.setDoOutput(true);
		c.setDoInput(true);
		c.setRequestProperty("Content-type", "application/xml");
		try( OutputStreamWriter out = new OutputStreamWriter(c.getOutputStream()) ){
			out.write("<credentials><username>admin</username><password>test</password></credentials>");
		}
		if( c.getResponseCode() != 200 ) {
			throw new IOException("Failed to authenticate as admin/test. HTTP response code "+c.getResponseCode());
		}
		try( InputStream in = c.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))){
			return reader.readLine();
		}
	}

	private void addDemoRequests() throws MalformedURLException, IOException {
		// get authentication token
		String authToken = retrieveAdminAuthToken();
		System.out.println("Retrieved token for admin authentication: "+authToken);
		BrokerAdmin admin = new BrokerAdmin(http.getBrokerServiceURI());
		admin.setClientAuthenticator(HttpApiKeyAuth.newBearer(authToken));
		
		if( admin.listAllRequests().isEmpty() ) {
			// make sure at least one test request exists
			int num = admin.createRequest("application/x.test.request", "test 1");
			admin.publishRequest(num);
			
			num = admin.createRequest("application/x.test.request", "test 2");
			admin.publishRequest(num);
		}

	}

	public void simulateNodes() throws IOException {
		// add some nodes
		BrokerClient c = new BrokerClient(http.getBrokerServiceURI());
		c.setClientAuthenticator(HttpApiKeyAuth.newBearer("xxxApiKey123"));
		try( InputStream in = TestServer.class.getResourceAsStream("/stats-example1.xml") ){
			c.putMyResource("stats", "application/xml", in);
		}
		try( InputStream in = TestServer.class.getResourceAsStream("/properties-example1.xml") ){
			c.putMyResource("versions", "application/xml", in);
		}
		// retrieve request 0
		c.postRequestStatus(0, RequestStatus.retrieved);
		c.postRequestFailed(0, "Request failed test", new RuntimeException("Test exception"));

		// second node
		c.setClientAuthenticator(HttpApiKeyAuth.newBearer("xxxApiKey567"));
		try( InputStream in = TestServer.class.getResourceAsStream("/stats-example2.xml") ){
			c.putMyResource("stats", "application/xml", in);
		}
		try( InputStream in = TestServer.class.getResourceAsStream("/properties-example2.xml") ){
			c.putMyResource("versions", "application/xml", in);
		}
		c.postRequestStatus(0, RequestStatus.retrieved);
		c.postRequestStatus(0, RequestStatus.interaction);

		// third node
		c.setClientAuthenticator(HttpApiKeyAuth.newBearer("xxxApiKey890"));
		c.postRequestStatus(0, RequestStatus.retrieved);
		c.postRequestStatus(0, RequestStatus.queued);
		c.postRequestStatus(0, RequestStatus.processing);
		// submit data
		c.putRequestResult(0, "application/x.test.result", "result 3");
		c.postRequestStatus(0, RequestStatus.completed);

		
	}


}
