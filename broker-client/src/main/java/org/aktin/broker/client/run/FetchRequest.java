package org.aktin.broker.client.run;

import java.io.IOException;
import java.net.URI;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.auth.ApiKeyAuthentication;

/**
 * Fetch requests.
 * @author R.W.Majeed
 *
 */
public class FetchRequest {

	/**
	 * Fetch requests.
	 * @param args command line arguments. First three are required: Broker URL,  API key, request ID and media type to fetch.
	 * @throws IOException IO Error
	 */
	public static void main(String[] args) throws IOException{
		String apiKey = args[1];
		String brokerUrl = args[0];
		int requestId;
		try{
			requestId = Integer.parseInt(args[2]);
		}catch( NumberFormatException e ){
			System.err.println("Unable to parse request id: "+args[2]+": "+e.getMessage());
			System.exit(-1);
			return;
		}
		String mediaType = args[3];
		
		BrokerClient2 client = new BrokerClient2(URI.create(brokerUrl));
		client.setAuthFilter(new ApiKeyAuthentication(apiKey));
		String s = client.getMyRequestDefinitionString(requestId, mediaType);
		System.out.print(s);
	}
}
