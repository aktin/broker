package org.aktin.broker.auth;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.aktin.broker.db.BrokerBackend;


@Singleton
public class AuthCache {

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
		return p;
	}
}
