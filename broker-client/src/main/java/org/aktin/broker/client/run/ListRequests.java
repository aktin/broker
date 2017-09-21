package org.aktin.broker.client.run;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.broker.xml.RequestInfo;

/**
 * Fetch requests.
 * @author R.W.Majeed
 *
 */
public class ListRequests {

	/**
	 * Fetch requests.
	 * @param args command line arguments. First two are required: Broker URL and API key.
	 * @throws IOException IO Error
	 */
	public static void main(String[] args) throws IOException{
		String apiKey = args[1];
		String brokerUrl = args[0];
		BrokerClient client = new BrokerClient(URI.create(brokerUrl));
		client.setClientAuthenticator(HttpApiKeyAuth.newBearer(apiKey));
		System.out.println("Connecting to broker "+brokerUrl);
		List<RequestInfo> rl = client.listMyRequests();
		System.out.println("Number of requests "+rl.size());
		for( RequestInfo i : rl ){
			System.out.println("Request #"+i.getId());
			System.out.println("\ttargeted="+i.targeted);
			System.out.println("\tpublished="+i.published);
			System.out.println("\ttypes="+Arrays.toString(i.types));
			System.out.println();
		}
	}
}
