package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aktin.broker.client.live.ClientConfiguration;
import org.aktin.broker.client2.validator.RequestValidatorFactory;

import lombok.Data;

@Data
public abstract class AbstractClientConfiguration implements ClientConfiguration{
	String requestMediatype;
	String resultMediatype;
	URI brokerEndpointURI;
	String authClass;
	String authParam;

	RequestValidatorFactory requestValidator;

	int websocketReconnectSeconds;
	boolean websocketReconnectPolling;
	int websocketPingpongSeconds;
	
	int executorThreads;
	private long executorTimeoutMillis;



	
	public AbstractClientConfiguration(InputStream in) throws IOException {
		Properties props = new Properties();
		props.load(in);
		
		String mapvars = props.getProperty("properties.mapvars",""); 
		if( mapvars.equals("") || mapvars.equals(Boolean.FALSE.toString()) ){
			// no mapping
		}else {
			Function<String,String> mapping;
			if( mapvars.equals("env") ) {
				mapping = System.getenv()::get;
			}else if( mapvars.equals("system") ) {
				mapping = System::getProperty;
			}else {
				throw new IllegalArgumentException("Illegal value for properties.mapvars. Use one of false,env,system");
			}
			// map environment variable placeholders in all properties
			Enumeration<Object> keys = props.keys();
			while( keys.hasMoreElements() ) {
				String key = keys.nextElement().toString();
				String value = props.getProperty(key);
				props.setProperty(key, lookupPlaceholders(value, mapping));
			}
		}

		this.requestMediatype = props.getProperty("broker.request.mediatype");
		this.resultMediatype = props.getProperty("broker.result.mediatype");
		
		// request validator
		String validatorClass = props.getProperty("broker.request.validator.class");
		if( validatorClass != null ) {
			this.requestValidator = loadValidatorClass(validatorClass);
		}
		
		try {
			this.brokerEndpointURI = new URI(props.getProperty("broker.endpoint.uri"));
		} catch (URISyntaxException e) {
			throw new IOException("Broker endpoint URI syntax invalid",e);
		}
		this.authClass = props.getProperty("client.auth.class");
		this.authParam = props.getProperty("client.auth.param");
		
		this.websocketReconnectSeconds = Integer.valueOf(props.getProperty("client.websocket.reconnect.seconds"));
		this.websocketReconnectPolling = Boolean.valueOf(props.getProperty("client.websocket.reconnect.polling"));
		this.websocketPingpongSeconds = Integer.valueOf(props.getProperty("client.websocket.ping.seconds","0"));
		
		this.executorThreads = Integer.valueOf(props.getProperty("client.executor.threads","1"));
		this.executorTimeoutMillis = 1000*Long.valueOf(props.getProperty("client.executor.timeout.seconds"));

		loadConfig(props);
		
	}
	
	protected abstract void loadConfig(Properties properties);
	
	@SuppressWarnings("unchecked")
	private static RequestValidatorFactory loadValidatorClass(String className) throws IOException{
		Class<RequestValidatorFactory> clazz;
		try{
			clazz = (Class<RequestValidatorFactory>) Class.forName(className);
			return clazz.getConstructor().newInstance();
		}catch( ClassNotFoundException e ) {
			throw new RuntimeException("Validator class not found "+className, e);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
			throw new RuntimeException("Unable to instatiate validator factory "+className+" via no-args constructor", e);
		} 
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


}
