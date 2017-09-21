package org.aktin.broker.client.run;

import java.io.IOException;
import java.net.URI;

import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;

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
		
		BrokerClient client = new BrokerClient(URI.create(brokerUrl));
		client.setClientAuthenticator(HttpApiKeyAuth.newBearer(apiKey));
		String s = client.getMyRequestDefinitionString(requestId, mediaType);
		System.out.print(s);
	}
}
