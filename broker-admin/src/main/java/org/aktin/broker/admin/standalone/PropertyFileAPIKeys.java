package org.aktin.broker.admin.standalone;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;

import org.aktin.broker.auth.AuthFilterAPIKeys;
import org.aktin.broker.rest.Authenticated;

@Singleton
@Authenticated
@Provider
@Priority(Priorities.AUTHENTICATION)
public class PropertyFileAPIKeys extends AuthFilterAPIKeys {
	private static final Logger log = Logger.getLogger(PropertyFileAPIKeys.class.getName());

	private Properties properties;

	public PropertyFileAPIKeys(InputStream in) throws IOException {
		properties = new Properties();
		properties.load(in);
		log.info("Loaded "+properties.size()+" client API keys");
	}

	@Override
	public String getClientDN(String apiKey) {
		return properties.getProperty(apiKey);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String,String> getMap(){
		return (Map)properties;
	}
}
