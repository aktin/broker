package org.aktin.broker.client.live;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Properties;

import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.client2.BrokerClient2;



//@Log
public class CLI implements Runnable {
	private CLIExecutionService<?> exec;

	public CLI(CLIExecutionService<?> plugin, BrokerClient2 client)  {
		this.exec = plugin;
	}

	public static CLIClientPluginConfiguration<?> loadPlugin(String className, InputStream config) throws ClassNotFoundException, InvocationTargetException{
		@SuppressWarnings("unchecked")
		Class<CLIClientPluginConfiguration<?>> clazz = (Class<CLIClientPluginConfiguration<?>>) Class.forName(className);
		CLIClientPluginConfiguration<?> plugin;
		try {
			plugin = clazz.getConstructor(InputStream.class).newInstance(config);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| NoSuchMethodException | SecurityException e) {
			throw new ClassNotFoundException("Plugin configuration constructor failed", e);
		}
		return plugin;
	}

	public static void main(String[] args) {
		if( args.length != 2 ) {
			System.err.println("Usage: "+CLI.class.getPackageName()+"."+CLI.class.getName()+" <pluginconfigclass> <configproperties>");
			return;
		}

		// load plugin config
		CLIClientPluginConfiguration<?> config;
		try( InputStream in = Files.newInputStream(Paths.get(args[1])) ){
			config = loadPlugin(args[0], in);
		}catch( ClassNotFoundException e ) {
			e.printStackTrace();
			System.err.println();
			System.err.println("Unable to instatiate plugin "+args[0]);
			return;
		}catch( InvocationTargetException e ) {
			e.printStackTrace();
			System.err.println();
			System.err.println("Exception encountered during init of plugin "+args[0]);
			return;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println();
			System.err.println("Unable to read configuration "+args[1]);
			return;
		}
		
		BrokerClient2 client;
		try{
			AuthFilter auth = config.instantiateAuthFilter();
			client = initializeClient(config, auth);
		}catch( IllegalArgumentException e ) {
			e.printStackTrace();
			System.err.println();
			System.out.println("Unable to instantiate AuthFilter "+config.getAuthClass());
			return;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println();
			System.out.println("Client communication to broker failed: "+e.getMessage());
			return;
		}
		// basic initialization successful, run the service 
		CLIExecutionService<?> service = config.createService(client);
		
		new CLI(service, client).run();		
	}

	private static BrokerClient2 initializeClient(CLIClientPluginConfiguration<?> config, AuthFilter auth) throws IOException {
		BrokerClient2 client = new BrokerClient2(config.getBrokerEndpointURI());
		client.setAuthFilter(auth);
		// post startup versions to broker
		Properties props = new Properties();
		// first, collect relevant system properties
		for( String syskey : new String[] {"os.name","os.version","java.vendor","java.version"})
		props.put(syskey, System.getProperty(syskey));
		// add additional information
		Package pkg = CLI.class.getPackage(); 
		props.put("client.version", pkg.getImplementationTitle()+" "+pkg.getImplementationVersion());
		props.put("client.startup", Instant.now().toString());

		// if this method fails, it serves an early indicator of connection problems
		client.putMyResourceProperties("versions", props);

		return client;
	}

	@Override
	public void run() {
		// add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.err.println("shutdown initiated..");
				System.err.flush();
				exec.shutdown();
			}
		});

		exec.run();
			
	}
}
