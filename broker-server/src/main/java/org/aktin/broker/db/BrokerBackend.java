package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.server.Broker;
import org.aktin.broker.server.auth.AuthInfo;

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
	 * @param auth authentication information
	 * @return principal
	 * @throws SQLException SQL error
	 */
	Principal accessPrincipal(AuthInfo auth) throws SQLException;

	void updateNodeLastSeen(int[] nodeIds, long[] timestamps) throws SQLException;
	
	default void updateNodeLastSeen(Map<Integer,Long> timestamps) throws SQLException{
		// convert map to arrays and call original method
		int[] ids = new int[timestamps.size()];
		long[] ts = new long[timestamps.size()];
		int i=0;
		for( Entry<Integer, Long> e : timestamps.entrySet() ) {
			ids[i] = e.getKey();
			ts[i] = e.getValue();
			i++;
		}
		updateNodeLastSeen(ids, ts);
	}


	void updateNodeResource(int nodeId, String resourceId, MediaType mediaType, InputStream content) throws SQLException, IOException;


	void clearDataDirectory() throws IOException;
}