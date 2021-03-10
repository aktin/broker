package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Properties;
import java.util.concurrent.Executors;

import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.client2.BrokerClient2;



//@Log
public class CLI implements Runnable {
	private ProcessExecutionService exec;

	public CLI(ProcessExecutionConfig config, BrokerClient2 client)  {
		this.exec = new ProcessExecutionService(client, config, Executors.newSingleThreadExecutor());
	}


	public static void main(String[] args) {
		if( args.length != 1 ) {
			System.err.println("Usage: "+CLI.class.getPackageName()+"."+CLI.class.getName()+" <configproperties>");
			return;
		}
		ProcessExecutionConfig cfg;
		try( InputStream in = Files.newInputStream(Paths.get(args[0])) ){
			cfg = new ProcessExecutionConfig(in);
		} catch (IOException e) {
			System.err.println("Unable to read configproperties "+args[0]+": "+e.getMessage());
			return;
		}
		BrokerClient2 client;
		try{
			AuthFilter auth = cfg.instantiateAuthFilter();
			client = initializeClient(cfg, auth);
		}catch( IllegalArgumentException e ) {
			e.printStackTrace();
			System.err.println();
			System.out.println("Unable to instantiate AuthFilter "+cfg.getAuthClass());
			return;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println();
			System.out.println("Client communication to broker failed: "+e.getMessage());
			return;
		}
		// basic initialization successful, run the service 
		new CLI(cfg, client).run();		
	}

	private static BrokerClient2 initializeClient(ProcessExecutionConfig config, AuthFilter auth) throws IOException {
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
