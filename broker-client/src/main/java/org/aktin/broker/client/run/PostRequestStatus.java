package org.aktin.broker.client.run;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.auth.ApiKeyAuthentication;
import org.aktin.broker.xml.RequestStatus;

/**
 * Post request status
 * @author R.W.Majeed
 *
 */
public class PostRequestStatus {

	/**
	 * Post request status. For allowed values, see {@link RequestStatus}
	 * @param args command line arguments. First three are required: Broker URL,  API key, request ID and status to post
	 * As optional fourth argument, a message/description can be submitted to the server. 
	 * @throws IOException IO Error
	 */
	public static void main(String[] args) throws IOException{
		if( args.length < 4 || args.length > 5 ){
			System.out.println("Usage: PostRequestStatus <brokerurl> <apikey> <requestid> <status> [<message>]");
			System.exit(-1);
			return;
		}
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
		String message = null;
		if( args.length == 5 ){
			message = args[4].replace("\\n", "\n").replace("\\t", "\t");
			System.out.println("Posting message:"+message);
		}
		BrokerClient2 client = new BrokerClient2(URI.create(brokerUrl));
		client.setAuthFilter(new ApiKeyAuthentication(apiKey));
		if( message == null ){
			client.postRequestStatus(requestId, status);
		}else{
			client.postRequestStatus(requestId, status, Instant.now(), message);
		}
	}
}
