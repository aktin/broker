package org.aktin.broker.admin.standalone;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.sql.DataSource;

import org.aktin.broker.auth.CascadedAuthProvider;
import org.aktin.broker.server.auth.AuthProvider;

import lombok.extern.java.Log;

/**
 * Configuration options can be specified/overridden via system properties:
 * <p><ul>
 * <li> {@code aktin.broker.auth.provider} use one or more authentication provider implementations. separated by comma.
 * <li> {@code aktin.broker.websocket.idletimeoutseconds} number of seconds to keep websocket connections open without data.
 * <li> {@code aktin.broker.jdbc.datasource.class} datasource implementation class for database driver defaults to local embedded HSQL database
 * <li> {@code aktin.broker.jdbc.url} JDBC URL to use with the datasource class. defaults to local embedded HSQL database
 * 
 * @author Raphael
 *
 */
@Log
public class DefaultConfiguration implements Configuration{
	private static final String DEFAULT_AUTH_PROVIDER = "org.aktin.broker.auth.apikey.ApiKeyPropertiesAuthProvider,org.aktin.broker.auth.cred.CredentialTokenAuthProvider";
	private AuthProvider authProvider;

	
	public DefaultConfiguration() {
		String ap = System.getProperty("aktin.broker.auth.provider", DEFAULT_AUTH_PROVIDER);

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
	@Override
	public long getWebsocketIdleTimeoutMillis() {
		return 1000 * Integer.valueOf(
					System.getProperty("aktin.broker.websocket.idletimeoutseconds", Integer.toString(60*60*2))
				);
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
	@Override
	public Class<? extends DataSource> getJdbcDataSourceClass() throws ClassNotFoundException {
		// also possible org.postgresql.ds.PGSimpleDataSource or PGPoolingDataSource
		String name = System.getProperty("aktin.broker.jdbc.datasource.class");
		Class<? extends DataSource> clazz;
		if( name != null ) {
			clazz = Class.forName(name).asSubclass(DataSource.class);
		}else {
			clazz = getDefaultHsqlDataSource();
		}
		return clazz;
	}

	public static String getDefaultHsqlJdbcUrl(Path basePath) {
		String path = basePath.resolve("broker").toString();
		return "jdbc:hsqldb:file:"+path+";shutdown=false;user=admin;password=secret";
	}
	public static Class<? extends DataSource> getDefaultHsqlDataSource(){
		return org.hsqldb.jdbc.JDBCDataSource.class;
	}
	@Override
	public String getJdbcUrl() {
		String url = System.getProperty("aktin.broker.jdbc.url");
		if( url == null ) {
			url = getDefaultHsqlJdbcUrl(getBasePath());
			log.info("Generating JDBC URL for HSQLDB: "+url);
		}else {
			log.info("Using JDBC URL from system properties: "+url);
		}
		return url;
	}

}
