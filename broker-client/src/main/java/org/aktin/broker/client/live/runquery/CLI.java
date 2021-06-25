package org.aktin.broker.client.live.runquery;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.aktin.broker.client.live.BrokerConfiguration;
import org.aktin.broker.client2.BrokerAdmin2;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class CLI  {
	private BrokerAdmin2 admin;

	private static void printUsage() {
		System.err.println("Usage: "+CLI.class.getPackageName()+"."+CLI.class.getName()+" <BROKER_ENDPOINT_URI> <AuthFilterImplementationClass> <AuthFilterArgument> <RequestMediaType> <RequestFileName>");
		System.err.println("e.g.: "+CLI.class.getPackageName()+"."+CLI.class.getName()+" http://localhost:8080/broker/ org.aktin.broker.client2.auth.ApiKeyAuthentication xxxAdmin1234 text/sql query.sql");			
//		System.err.println("Usage: "+CLI.class.getPackageName()+"."+CLI.class.getName()+" [-waitfor=N] -broker=BROKERURI [-auth=CLASS,PARAM] MEDIATYPE1=INPUT1 [...]");
//		System.err.println(" -waitfor specifies the number of nodes to wait for. defaults to -1 meaning all nodes currently known.");
//		System.err.println(" -broker specifies the broker endpoint URI e.g. http://localhost:8080/broker/");
//		System.err.println(" -auth: use authentication class and param. e.g. org.aktin.broker.client2.auth.ApiKeyAuthentication,xxxApiKey123");
//		System.err.println(" MEDIATYPE1=INPUT1: request definition to publish. if only one type is specified, INPUT1 can be '-' to read from stdin. Otherwise INPUT1 should indicate a file containing the definition");		
	}

	@Getter
	@AllArgsConstructor
	public static class Config implements BrokerConfiguration{
		private URI brokerEndpointURI;
		private String authClass;
		private String authParam;		
	}
	public void runQuery(String mediaType, String requestContent) throws IOException {
		int rid = admin.createRequest(mediaType, requestContent);
		admin.publishRequest(rid);
		System.out.println("Request created and published: "+rid);
	}
	public static void main(String[] args) {
		if( args.length != 5 ) {
			printUsage();
			return;
		}
		String mediaType = args[3];

		Config config = new Config(URI.create(args[0]), args[1], args[2]);
		BrokerAdmin2 admin = new BrokerAdmin2(config.getBrokerEndpointURI());
		admin.setAuthFilter(config.instantiateAuthFilter());

		CLI cli = new CLI();
		cli.admin = admin;
		try {
			String requestContent = Files.readString(Paths.get(args[4]));
			cli.runQuery(mediaType, requestContent);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
