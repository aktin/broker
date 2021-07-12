package org.aktin.broker.client.live.util;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.aktin.broker.client.live.BrokerConfiguration;
import org.aktin.broker.client2.BrokerAdmin2;
import org.aktin.broker.xml.RequestInfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class RequestList  {
	private BrokerAdmin2 admin;

	private static void printUsage() {
		System.err.println("Usage: "+RequestList.class.getPackageName()+"."+RequestList.class.getName()+" <BROKER_ENDPOINT_URI> <AuthFilterImplementationClass> <AuthFilterArgument> <RequestMediaType> <XPathPredicate>");
		System.err.println("e.g.: "+RequestList.class.getPackageName()+"."+RequestList.class.getName()+" http://localhost:8080/broker/ org.aktin.broker.client2.auth.ApiKeyAuthentication xxxAdmin1234 text/xml //tag='bla'");			
	}

	@Getter
	@AllArgsConstructor
	public static class Config implements BrokerConfiguration{
		private URI brokerEndpointURI;
		private String authClass;
		private String authParam;		
	}
	public void runQuery(String mediaType, String xpathExpression) throws IOException {
		
		List<RequestInfo> list = admin.listRequestsFiltered(mediaType, xpathExpression);
		if( list == null || list.size() == 0 ) {
			System.out.println("no matching requests");
		}else {
			for( RequestInfo info : list ) {
				System.out.println("request "+info.getId());
			}
		}		
	}
	public static void main(String[] args) {
		if( args.length != 5 ) {
			printUsage();
			return;
		}
		String mediaType = args[3];
		String predicate = args[4];

		Config config = new Config(URI.create(args[0]), args[1], args[2]);
		BrokerAdmin2 admin = new BrokerAdmin2(config.getBrokerEndpointURI());
		admin.setAuthFilter(config.instantiateAuthFilter());

		RequestList cli = new RequestList();
		cli.admin = admin;
		try {
			cli.runQuery(mediaType, predicate);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
