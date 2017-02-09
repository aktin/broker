package org.aktin.broker.db;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.server.Broker;

public interface BrokerBackend extends Broker{

	void setBrokerDB(DataSource brokerDB);


	/**
	 * Add or retrieve the node principal. The node is identified by its key.
	 * For API-Key authentication, this can be the API key. For TLS client certificate
	 * authentication, this may be the certificate serial number.
	 * 
	 * @param nodeKey unique key for the node. E.g. Certificate serial number or API key
	 * @param clientDn optional X500 client DN string
	 * @return principal
	 * @throws SQLException SQL error
	 */
	Principal accessPrincipal(String nodeKey, String clientDn) throws SQLException;



	void updateNodeLastSeen(int[] nodeIds, long[] timestamps) throws SQLException;
}