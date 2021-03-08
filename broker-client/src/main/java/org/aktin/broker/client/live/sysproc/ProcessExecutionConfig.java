package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aktin.broker.client.live.Configuration;
import org.aktin.broker.client2.AuthFilter;

import lombok.Data;

@Data
public class ProcessExecutionConfig implements Configuration{
	String requestMediatype;
	String resultMediatype;
	URI brokerEndpointURI;
	String authClass;
	String authParam;

	int websocketReconnectSeconds;
	boolean websocketReconnectPolling;

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
		
		this.websocketReconnectSeconds = Integer.valueOf(props.getProperty("client.websocket.reconnect.seconds"));
		this.websocketReconnectPolling = Boolean.valueOf(props.getProperty("client.websocket.reconnect.polling"));
		this.processTimeoutMillis = 1000*Long.valueOf(props.getProperty("process.timeout.seconds"));
		
		String cmd = props.getProperty("process.command");
		if( props.getProperty("process.command.mapenv","").contentEquals(Boolean.TRUE.toString()) ){
			// map environment variable placeholders in command
			cmd = lookupPlaceholders(cmd, System.getenv()::get);
		}
		command = new ArrayList<>();
		command.add(cmd);
		command.addAll(Arrays.asList(props.getProperty("process.args").split("\\s+")));
	}

	public static String lookupPlaceholders(String command, Function<String, String> lookup) {
		Pattern p = Pattern.compile("\\$\\{[^\\}]+\\}");
		Matcher m = p.matcher(command);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String name = m.group();
			String val = lookup.apply(name.substring(2, name.length()-1));
			if( val == null ) {
				val = "";
			}
		    m.appendReplacement(sb, "");
		    sb.append(val);
		}
		m.appendTail(sb);
		return sb.toString();
	}

	public AuthFilter instantiateAuthFilter() throws IllegalArgumentException {
		try {
			Class<?> clazz = Class.forName(authClass);
			Constructor<? extends AuthFilter> constructor 
				= clazz.asSubclass(AuthFilter.class).getConstructor(String.class);
			return constructor.newInstance(authParam);
		} catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException e) {
			throw new IllegalArgumentException("Unable to instantiate AuthFilter for class "+authClass, e);
		}
	}

}
