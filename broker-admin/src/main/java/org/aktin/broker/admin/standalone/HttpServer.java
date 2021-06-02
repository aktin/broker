package org.aktin.broker.admin.standalone;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import javax.sql.DataSource;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.aktin.broker.Broker;
import org.aktin.broker.admin.rest.FormTemplateEndpoint;
import org.aktin.broker.db.LiquibaseWrapper;
import org.aktin.broker.server.auth.AuthProvider;
import org.aktin.broker.server.auth.HeaderAuthentication;
import org.aktin.broker.websocket.HeaderAuthSessionConfigurator;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;


import liquibase.exception.LiquibaseException;

/**
 * Run a standalone broker server from the command line.
 * <p>
 * To update the subject_DN in the database, specify a system 
 * property {@code rewriteNodeDN}. In this case, all stored node
 * subject DN values are rewritten with the values loaded from the
 * API key properties file.
 * </p>
 * @author R.W.Majeed
 *
 */
public class HttpServer {
	private Configuration config;
	private ResourceConfig rc;
	private Server jetty;
	private DataSource ds;
	private MyBinder binder;
	private AuthProvider authFactory;
	private HeaderAuthentication auth;

	public HttpServer(Configuration config) throws SQLException, IOException{
		this.config = config;
		this.authFactory = config.getAuthProvider();
		Objects.requireNonNull(authFactory,"No AuthProvider available!");
		authFactory.setBasePath(config.getBasePath());
		this.auth = authFactory.getInstance();
		Objects.requireNonNull(auth);
		// initialize database
		initialiseDatabase(config);
		rc = new ResourceConfig();
		// register broker services
		rc.registerClasses(Broker.ENDPOINTS);
		rc.registerClasses(Broker.AUTH_FILTERS);
		// register auth entdpoints
		rc.registerClasses(authFactory.getEndpoints());
		// register admin endpoints
		rc.register(FormTemplateEndpoint.class);
		// websocket endpoints are initialized in method #setupWebsockets
	}

	private void initialiseDatabase(Configuration config) throws SQLException{
		Method methodSetURL;
		Class<? extends DataSource> clazz = null;
		try {
			clazz = config.getJdbcDataSourceClass(); 
			this.ds = clazz.getConstructor().newInstance();
			// setURL method
			methodSetURL = clazz.getMethod("setURL", String.class);
			methodSetURL.invoke(this.ds, config.getJdbcUrl());
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | ClassNotFoundException e1) {
			throw new SQLException("Unable to instantiate database DataSource class or missing/failed setURL(String) method: "+clazz.getName());
		}
		
		try( LiquibaseWrapper w = new LiquibaseWrapper(ds.getConnection()) ){
			w.update();
		} catch (LiquibaseException e ) {
			throw new SQLException("Unable to initialise database", e);
		}
	}
	
	protected void start_local(int port) throws Exception{
		start(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
	}
	public URI getBrokerServiceURI(){
		return jetty.getURI().resolve(Broker.SERVICE_URL);
	}
	private Handler createStaticResourceHandler(){
		// use resource handler to serve files from WEB-INF
		// http://stackoverflow.com/questions/10284584/serving-static-files-w-embedded-jetty
		// TODO set context path to /aktin/admin

		ResourceHandler handler = new ResourceHandler();

		Resource res = Resource.newClassPathResource("webapp");
		// no need to check if res is null, since the folder is present in the source code
		handler.setBaseResource(res);
		handler.setDirectoriesListed(true);
		handler.setWelcomeFiles(new String[]{"index.html"});

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/admin/html");
		context.insertHandler(handler);
		
		return context;
	}
	public void start(InetSocketAddress addr) throws Exception{
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/"); // /admin/rest

		jetty = new Server(addr);
		HandlerList handlers = new HandlerList();
		handlers.addHandler(createStaticResourceHandler());
		handlers.addHandler(context);
		jetty.setHandler(handlers);

		ErrorHandler errorHandler = new ErrorHandler();
		errorHandler.setShowStacks(true);
		jetty.addBean(errorHandler);

		ServletHolder jersey = new ServletHolder(new ServletContainer(rc));
//		jersey.setInitOrder(0);
		context.addServlet(jersey, "/*");

		// initialise query manager
		this.binder = new MyBinder(ds,config, this.authFactory, this.auth);
		rc.register(this.binder);

		// setup websockets
		setupWebsockets(context);
		
		for( Object c : rc.getInstances() ) {
			System.out.println("Instance:"+c.getClass()+":"+c.toString());
		}
		jetty.start();
	}

	private void setupWebsockets(ServletContextHandler context) throws Exception{
		ServerContainer c = WebSocketServerContainerInitializer.initialize(context);
		// TODO verify session idle timeout and increase accordingly. e.g. 60 minutes 
		c.setDefaultMaxSessionIdleTimeout(config.getWebsocketIdleTimeoutMillis());
		// use HeaderAuthentication
		HeaderAuthSessionConfigurator sc = new HeaderAuthSessionConfigurator(this.auth, binder.getAuthCache());
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
	public void join() throws InterruptedException{
		jetty.join();
	}
	public void destroy() throws Exception{
		System.out.println("Shutting down database..");
		try( Connection dbc = ds.getConnection();
				Statement st = dbc.createStatement() ){
			st.executeUpdate("SHUTDOWN");
		}
		System.out.println("Cleanup jetty..");
		System.out.flush();
		try {
				jetty.destroy();
		}catch( Throwable e ) {
			System.out.println("Jetty.destroy failed with "+e);
		}
		// help cleanup
		binder.closeCloseables();
		// release threads waiting for termination
		synchronized( this ){
			this.notifyAll();
		}
	}
	public void stop() throws Exception{
		jetty.stop();
	}

	
	/**
	 * Run a standalone broker server from the command line.
	 *
	 * @param args command line arguments: port can be specified optionally
	 * @throws Exception any error
	 */
	public static void main(String[] args) throws Exception{
		// use port if specified
		int port;
		InetAddress bindaddr;
		if( args.length == 0 ){
			port = 8080;
			bindaddr = InetAddress.getLoopbackAddress();
		}else if( args.length == 1 ){
			int colon = args[0].indexOf(':');
			if( colon == -1 ){
				bindaddr = InetAddress.getLoopbackAddress();
				port = Integer.parseInt(args[0]);
			}else{
				bindaddr = InetAddress.getByName(args[0].substring(0, colon));
				port = Integer.parseInt(args[0].substring(colon+1));
			}
		}else{
			System.err.println("Too many command line arguments!");
			System.err.println("Usage: "+HttpServer.class.getCanonicalName()+" [[hostname:]port]");
			System.exit(-1);
			return;
		}

		
		// load hsql driver
		Class.forName("org.hsqldb.jdbcDriver");
		
		// start server
		final HttpServer server = new HttpServer(new DefaultConfiguration());
		// add shutdown hook
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				try {
					System.out.println("Executing shutdown hook..");
					System.out.flush();
					server.stop();
					// wait for cleanup to finish
					synchronized( server ){
						server.wait(3000);
					}
					System.out.println("Shutdown completed.");
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Failed to stop jetty during shutdown hook.");
				}
			}
		});

		try{
			server.start(new InetSocketAddress(bindaddr, port));
			System.err.println("Broker service at: "+server.getBrokerServiceURI());
			server.join();
		}finally{
			server.destroy();
		}
	}
}
