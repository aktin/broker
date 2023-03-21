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

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthRole;
import org.aktin.broker.xml.Node;


/**
 * In memory cache for user objects which also has manages a last-contact timestamp.

 * @author R.W.Majeed
 *
 */
@Singleton
public class AuthCache implements Flushable, Closeable{
	private static final Logger log = Logger.getLogger(AuthCache.class.getName());

	private Map<String, Principal> cache;

	private BrokerBackend backend;

	public AuthCache(){
		cache = new HashMap<>();
	}
	/**
	 * CDI constructor
	 * @param backend
	 */
	@Inject
	public AuthCache(BrokerBackend backend){
		this();
		setBackend(backend);
	}
	private void setBackend(BrokerBackend backend){
		this.backend = backend;
	}

	private boolean isNodePrincipal(AuthInfo info) {
		if( info.getRoles().contains(AuthRole.NODE_READ) || info.getRoles().contains(AuthRole.NODE_WRITE) ) {
			return true;
		}else {
			return false;
		}
	}
	/**
	 * Retrieve a {@link Principal} user object for a client node.
	 * @param info authentication info
	 * @return user object
	 * @throws SQLException 
	 */
	public Principal getPrincipal(AuthInfo info) throws IOException{
		Objects.requireNonNull(info);
		Objects.requireNonNull(info.getClientDN());
		Objects.requireNonNull(info.getUserId());
		Principal p = cache.get(info.getUserId());
		if( p == null ){
			// principal not cached previously
			if( isNodePrincipal(info) ) {
				// register node with backend				
				try {
					p = backend.accessPrincipal(info);
				} catch (SQLException e) {
					throw new IOException("SQL error during principal retrieval for node "+info.getUserId(), e);
				}
			}else {
				// admin user. just cache the information without registering with backend
				p = Principal.createAdminPrincipal(info);
			}
			cache.put(info.getUserId(), p);
		}else{
			; // TODO check if client DN changed. If so, log warning and update the client DN
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
		Map<Integer,Principal> lookup = new HashMap<>();
		// retrieve list cached principals which have been authenticated since startup
		for( Principal p : cache.values() ){
			//timestamps.put(p.getNodeId(), p.getLastAccessed());
			if( p.isNode() ) {
				lookup.put(p.getNodeId(), p);				
			}
		}
		for( Node node : nodes ){
			Principal p = lookup.get(node.id);
			if( p == null ) {
				// cached access information not available
				continue;
			}
			Long ts = p.getLastAccessed();
			if( ts != null ){
				// timesta mp not available
				node.lastContact = Instant.ofEpochMilli(ts);
			}
			node.websocket = (p.getWebsocketCount() > 0);
		}
	}
	// TODO also flush sometimes before close, e.g. using timer

	@PreDestroy
	@Override
	public void flush() throws IOException {
		// collect last accessed timestamps
		log.info("flushing");
		Map<Integer,Long> timestamps = new HashMap<>();
		
		for( Principal p : cache.values() ){
			if( p.isNode() ) {
				timestamps.put(p.getNodeId(), p.getLastAccessed());
			}
		}

		// convert to arrays
		// write last accessed timestamps to database
		try {
			backend.updateNodeLastSeen(timestamps);
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
