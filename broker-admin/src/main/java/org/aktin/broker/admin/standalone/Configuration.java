package org.aktin.broker.admin.standalone;

import java.io.IOException;
import java.nio.file.Path;

import org.aktin.broker.server.auth.AuthProviderFactory;

public interface Configuration {

	// configuration to specify external class implementing HeaderAuthentication
	AuthProviderFactory getAuthProvider() throws IOException;

	Path getBasePath();

	default String getDatabasePath() {return getBasePath().resolve("broker").toString();}
	default String getAggregatorDataPath() {return getBasePath().resolve("aggregator-data").toString();}
	default String getBrokerDataPath()  {return getBasePath().resolve("broker-data").toString();}
	default String getTempDownloadPath() {return getBasePath().resolve("download-temp").toString();}
	/**
	 * local TCP port to listen to
	 * @return port number
	 */
	int getPort();
}
