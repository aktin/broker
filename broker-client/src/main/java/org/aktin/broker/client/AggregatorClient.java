package org.aktin.broker.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.aktin.broker.client.BrokerClient.OutputWriter;

public class AggregatorClient extends AbstractClient {

	public AggregatorClient(URI endpointURI) {
		super(endpointURI);
	}

	public void putRequestResult(int requestId, String contentType, OutputWriter writer) throws IOException{
		HttpURLConnection c = openConnection("POST", "my/request/"+requestId+"/result");
		c.setDoOutput(true);
		c.setRequestProperty("Content-Type", contentType);
		try( OutputStream out = c.getOutputStream() ){
			writer.write(out);
		}
		c.getInputStream().close();
	}

}
