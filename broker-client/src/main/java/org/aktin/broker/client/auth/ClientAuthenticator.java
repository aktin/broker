package org.aktin.broker.client.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Interface for establishing authenticated connections.
 * This interface is deprecated. Use BrokerClient2/BrokerAdmin2
 * @author R.W.Majeed
 *
 */
@Deprecated
public interface ClientAuthenticator {

	
	/**
	 * Open an authenticated connection to the specified URL.
	 * The returned connection can be used to specify additional
	 * configuration like HTTP method and request headers.
	 * 
	 * @param url connection target
	 * @return connection, which is not yet connected.
	 * @throws IOException IO error
	 */
	HttpURLConnection openAuthenticatedConnection(URL url) throws IOException;
}
