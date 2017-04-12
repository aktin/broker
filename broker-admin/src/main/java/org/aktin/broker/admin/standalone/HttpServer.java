package org.aktin.broker.admin.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.aktin.broker.Broker;
import org.aktin.broker.admin.auth.AuthEndpoint;
import org.aktin.broker.admin.auth.AuthFilter;
import org.aktin.broker.admin.rest.ValidatorEndpoint;
import org.aktin.broker.db.LiquibaseWrapper;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import liquibase.exception.LiquibaseException;

public class HttpServer {
	private Configuration config;
	private ResourceConfig rc;
	private Server jetty;
	private DataSource ds;
	
	public HttpServer(Configuration config) throws SQLException, IOException{
		this.config = config;
		ds = new HSQLDataSource(config.getDatabasePath());
		// initialize database
		initialiseDatabase();
		rc = new ResourceConfig();
		try( InputStream in = config.readAPIKeyProperties() ){
			rc.register(new PropertyFileAPIKeys(in));			
		}
		// register broker services
		rc.registerClasses(Broker.ENDPOINTS);
		rc.register(AuthFilter.class);
		// register admin endpoints
		rc.register(AuthEndpoint.class);
		rc.register(ValidatorEndpoint.class);
	}

	private void initialiseDatabase() throws SQLException{
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
		
		handler.setBaseResource(Resource.newClassPathResource("webapp"));
		handler.setDirectoriesListed(true);
		handler.setWelcomeFiles(new String[]{"index.html"});

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/admin/html");
		
		context.setHandler(handler);
		
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

		ServletHolder jersey = new ServletHolder(new ServletContainer(rc));
//		jersey.setInitOrder(0);
		context.addServlet(jersey, "/*");

		// initialise query manager
		rc.register(new MyBinder(ds,config));

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

	
	/**
	 * Run the test server with with the official i2b2
	 * webclient.
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
		HttpServer server = new HttpServer(new DefaultConfiguration());
		try{
			server.start(new InetSocketAddress(bindaddr, port));
			System.err.println("Broker service at: "+server.getBrokerServiceURI());
			server.join();
		}finally{
			server.destroy();
		}
	}
}
