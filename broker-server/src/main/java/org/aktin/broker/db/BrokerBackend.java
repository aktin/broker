package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.server.Broker;

public interface BrokerBackend extends Broker{

	void setBrokerDB(DataSource brokerDB);


	/**
	 * Add or retrieve the node principal. The node is identified by its key. The key can 
	 * be any string value. It is used as primary identifier and should not change.
	 *
	 * For API-Key authentication, this can be the API key. For TLS client certificate
	 * authentication, this may be the certificate serial number. For external auth,
	 * a user id / login name can be used.
	 * 
	 * @param nodeKey unique key for the node. E.g. Certificate serial number or API key
	 * @param clientDn optional X500 client DN string
	 * @return principal
	 * @throws SQLException SQL error
	 */
	Principal accessPrincipal(String nodeKey, String clientDn) throws SQLException;

	void updateNodeLastSeen(int[] nodeIds, long[] timestamps) throws SQLException;


	void updateNodeResource(int nodeId, String resourceId, MediaType mediaType, InputStream content) throws SQLException, IOException;


	void clearDataDirectory() throws IOException;
}