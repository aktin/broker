package org.aktin.broker.auth;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.xml.Node;


@Singleton
public class AuthCache implements Flushable, Closeable{
	private static final Logger log = Logger.getLogger(AuthCache.class.getName());

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
		Objects.requireNonNull(clientDn);
		Principal p = cache.get(nodeKey);
		if( p == null ){
			p = backend.accessPrincipal(nodeKey, clientDn);
			cache.put(nodeKey, p);
		}else{
			// TODO check if client DN changed. If so, log warning and update the client DN
		}
		p.updateLastAccessed();
		return p;
	}

	/**
	 * Get the cached last contact timestamp. If the node did not have contact 
	 * since server startup, the node's timestamp will not be modified.
	 * @param nodes nodes to update the timestamp
	 */
	public void fillCachedAccessTimestamps(Iterable<Node> nodes){
		Map<Integer,Long> timestamps = new HashMap<>();
		for( Principal p : cache.values() ){
			timestamps.put(p.getNodeId(), p.getLastAccessed());
		}
		for( Node node : nodes ){
			Long ts = timestamps.get(node.id);
			if( ts != null ){
				node.lastContact = Instant.ofEpochMilli(ts);
			}
		}
	}
	// TODO also flush sometimes before close, e.g. using timer

	@PreDestroy
	@Override
	public void flush() throws IOException {
		// collect last accessed timestamps
		log.info("flushing");
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
	@Override
	public void close() throws IOException {
		log.info("performing close");
		flush();
	}

}
