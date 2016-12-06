package org.aktin.broker.auth;

import java.io.Flushable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.aktin.broker.db.BrokerBackend;


@Singleton
public class AuthCache implements Flushable{

	private Map<String, Principal> cache;

	private BrokerBackend backend;

	public AuthCache(){
		cache = new HashMap<>();
	}
	@Inject
	public AuthCache(BrokerBackend backend){
		this();
		setBackend(backend);
	}
	private void setBackend(BrokerBackend backend){
		this.backend = backend;
	}
	
	public Principal getPrincipal(String nodeKey, String clientDn) throws SQLException{
		Principal p = cache.get(nodeKey);
		if( p == null ){
			p = backend.accessPrincipal(nodeKey, clientDn);
			cache.put(nodeKey, p);
		}
		p.updateLastAccessed();
		return p;
	}

	// TODO also flush sometimes before close

	@PreDestroy
	@Override
	public void flush() throws IOException {
		// collect last accessed timestamps
		int[] nodeIds = new int[cache.size()];
		long[] timestamps = new long[cache.size()];
		int i=0;
		for( Principal p : cache.values() ){
			nodeIds[i] = p.getNodeId();
			timestamps[i] = p.getLastAccessed();
		}
		// write last accessed timestamps to database
		try {
			backend.updateNodeLastSeen(nodeIds, timestamps);
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

}
