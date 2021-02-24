package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.aktin.broker.client.live.Configuration;

import lombok.Data;

@Data
public class ProcessExecutionConfig implements Configuration{
	String requestMediatype;
	String resultMediatype;
	URI brokerEndpointURI;
	String authClass;
	String authParam;

	// specific config
	private long processTimeoutMillis;
	private List<String> command;
	

	
	public ProcessExecutionConfig(InputStream in) throws IOException {
		Properties props = new Properties();
		props.load(in);
		this.requestMediatype = props.getProperty("broker.request.mediatype");
		this.resultMediatype = props.getProperty("broker.result.mediatype");
		try {
			this.brokerEndpointURI = new URI(props.getProperty("broker.endpoint.uri"));
		} catch (URISyntaxException e) {
			throw new IOException("Broker endpoint URI syntax invalid",e);
		}
		this.authClass = props.getProperty("client.auth.class");
		this.authParam = props.getProperty("client.auth.param");
		
		this.processTimeoutMillis = Long.valueOf(props.getProperty("process.timeoutmillis"));
		command = new ArrayList<>();
		command.add(props.getProperty("process.command"));
		command.addAll(Arrays.asList(props.getProperty("process.args").split("\\s+")));
	}
}
