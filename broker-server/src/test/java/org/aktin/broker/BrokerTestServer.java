package org.aktin.broker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.sql.DataSource;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.aktin.broker.auth.AuthenticationRequestFilter;
import org.aktin.broker.auth.AuthorizationRequestFilter;
import org.aktin.broker.db.TestDataSource;
import org.aktin.broker.db.TestDatabaseHSQL;
import org.aktin.broker.server.auth.HeaderAuthentication;
import org.aktin.broker.websocket.HeaderAuthSessionConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * li2b2 server for unit tests
 * or demonstrations.
 * 
 * @author R.W.Majeed
 *
 */
public class BrokerTestServer {

	private ResourceConfig rc;
	private Server jetty;
	private DataSource ds;
	private HeaderAuthentication headerAuth;
	private MyBinder binder;
	
	public BrokerTestServer(HeaderAuthentication headerAuth) throws IOException, SQLException{
		this.headerAuth = headerAuth;
		ds = new TestDataSource(new TestDatabaseHSQL());
		rc = new ResourceConfig();
//		if( apiKeyAuth ){
//			// use API key auth
//			rc.register(ApiKeyAuth.class);
//		}else{
//			// use SSL header auth
//			rc.register(SSLHeaderAuth.class);
//		}
		//rc.register(headerAuth);
		rc.register(AuthenticationRequestFilter.class);
		rc.register(AuthorizationRequestFilter.class);
		rc.registerClasses(Broker.ENDPOINTS);
		//rc.registerClasses(Broker.WEBSOCKETS);

//		rc.registerClasses(BrokerWebsocket.class);
//		rc.register(MyBrokerEndpoint.class);
//		rc.register(NodeInfoEndpoint.class);
//		rc.register(RequestAdminEndpoint.class);		
//		rc.register(AggregatorEndpoint.class);
		this.binder = new MyBinder(ds, headerAuth);
		rc.register(binder);
	}
	public void register(Class<?> componentClass){
		rc.register(componentClass);
	}
	
	protected void start_local(int port) throws Exception{
		start(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
	}
	public URI getBrokerServiceURI(){
		return jetty.getURI().resolve(Broker.SERVICE_URL);
	}
	public int getLocalPort(){
		return ((ServerConnector)jetty.getConnectors()[0]).getLocalPort();
	}
	private void setupWebSockets(ServletContextHandler context, HeaderAuthentication auth) throws DeploymentException, ServletException{
		//WebSocketServerContainerInitializer.configureContext(context);
		ServerContainer c = WebSocketServerContainerInitializer.initialize(context);
		//ServerContainer c = (ServerContainer)context.getAttribute( javax.websocket.server.ServerContainer.class.getName() );
		HeaderAuthSessionConfigurator sc = new HeaderAuthSessionConfigurator(auth, binder.getAuthCache());
		for( Class<?> websocketClass : Broker.WEBSOCKETS ) {
			// retrieve path
			String restPath = websocketClass.getAnnotation(ServerEndpoint.class).value();
			// add websocket endpoint to server with custom authenticator
			c.addEndpoint(ServerEndpointConfig.Builder
					.create(websocketClass, restPath)
					.configurator(sc)
					.build()
			);
		}
	}
	public void start(InetSocketAddress addr) throws Exception{
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");

		jetty = new Server(addr);
		jetty.setHandler(context);

		ServletHolder jersey = new ServletHolder(new ServletContainer(rc));
//		jersey.setInitOrder(0);
		context.addServlet(jersey, "/*");
//		WebSocketServlet wss = new WebSocketServlet() {
//			@Override
//			public void configure(WebSocketServletFactory factory) {
//				factory.register(BrokerWebsocket.class);
//			}
//		};
//		context.addServlet(new ServletHolder(wss), "/*");
		setupWebSockets(context, headerAuth);
		jetty.start();
	}
	public void join() throws InterruptedException{
		jetty.join();
	}
	public void destroy() throws Exception{
		jetty.destroy();
	}
	public void stop() throws Exception{
		jetty.stop();
	}

	public DataSource getDataSource(){
		return ds;
	}
	/**
	 * Run the test server with with the official i2b2
	 * webclient.
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
			System.err.println("Usage: "+BrokerTestServer.class.getCanonicalName()+" [port]");
			System.exit(-1);
			return;
		}

		// start server
		BrokerTestServer server = new BrokerTestServer(new ApiKeyAuth());
		try{
			server.start(new InetSocketAddress(port));
			System.err.println("Broker service at: "+server.getBrokerServiceURI());
			server.join();
		}finally{
			server.destroy();
		}
	}
}
