package org.aktin.broker.client.run;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.broker.xml.RequestStatus;

/**
 * Post request status
 * @author R.W.Majeed
 *
 */
public class PostRequestStatus {

	/**
	 * Post request status
	 * @param args command line arguments. First three are required: Broker URL,  API key, request ID and status to post
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
		RequestStatus status;
		try{
			status = RequestStatus.valueOf(args[3]);
		}catch( IllegalArgumentException e ){
			System.err.println("Unable to parse request status: "+args[3]);
			System.err.println("Allowed values: "+Arrays.toString(RequestStatus.values()));
			System.exit(-1);
			return;
		}
		
		BrokerClient client = new BrokerClient(URI.create(brokerUrl));
		client.setClientAuthenticator(HttpApiKeyAuth.newBearer(apiKey));
		client.postRequestStatus(requestId, status);
	}
}
