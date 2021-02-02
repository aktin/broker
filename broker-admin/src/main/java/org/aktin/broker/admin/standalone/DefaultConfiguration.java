package org.aktin.broker.admin.standalone;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.aktin.broker.auth.CascadedAuthProvider;
import org.aktin.broker.server.auth.AuthProvider;

public class DefaultConfiguration implements Configuration{
	private static final String DEFAULT_AUTH_PROVIDER = "org.aktin.broker.auth.apikey.ApiKeyPropertiesAuthProvider,org.aktin.broker.auth.cred.CredentialTokenAuthProvider";
	private AuthProvider authProvider;

	
	public DefaultConfiguration() {
		String ap = System.getProperty("broker.auth.provider", DEFAULT_AUTH_PROVIDER);

		String[] aps = ap.split(",");
		if( aps.length == 1 ) {
			// single auth provider
			authProvider = loadProviderClass(aps[0]);
		}else {
			// multiple auth providers
			AuthProvider[] provs = new AuthProvider[aps.length];
			for( int i=0; i<provs.length; i++ ) {
				provs[i] = loadProviderClass(aps[i].trim());
			}
			authProvider = new CascadedAuthProvider(Arrays.asList(provs));
		}
	}
	private AuthProvider loadProviderClass(String name) throws IllegalArgumentException {
		Constructor<?> c;
		try {
			c = Class.forName(name).getConstructor();
			return (AuthProvider)c.newInstance();
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException("Unable to instantiate auth provider class: "+name, e);
		}
	}

	@Override
	public int getPort() {
		return 8080;
	}

//	@Override
//	public String getDatabasePath() {
//		return "broker";
//	}
//	@Override
//	public String getAggregatorDataPath() {
//		return "aggregator-data";
//	}
//	@Override
//	public String getBrokerDataPath() {
//		return "broker-data";
//	}
//
//	@Override
//	public String getTempDownloadPath() {
//		return "download-temp";
//	}

	@Override
	public AuthProvider getAuthProvider() {
		return authProvider;
	}

	@Override
	public Path getBasePath() {
		return Paths.get(".");
	}

}
