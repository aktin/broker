package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

public class PropertyFileAPIKeys extends HttpBearerAuthentication {
	private static final Logger log = Logger.getLogger(PropertyFileAPIKeys.class.getName());

	private Properties properties;

	public PropertyFileAPIKeys(InputStream in) throws IOException {
		properties = new Properties();
		properties.load(in);
		log.info("Loaded "+properties.size()+" client API keys");
	}

	@Override
	protected AuthInfo lookupAuthInfo(String token) throws IOException {
		String clientDn = properties.getProperty(token);
		if( clientDn == null ) {
			// unauthorized
			return null;
		}
		return new AuthInfoImpl(token, clientDn, HttpBearerAuthentication.defaultRolesForClientDN(clientDn));
	}
}
