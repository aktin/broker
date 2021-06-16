package org.aktin.broker.auth;

import java.util.Set;

import javax.ws.rs.core.SecurityContext;

import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthRole;

public class Principal implements java.security.Principal, SecurityContext{

	/**
	 * integer node id, if the principal is a node. {@code null otherwise}.
	 */
	private Integer nodeId;
	private String commonName;
	private String clientDn;
	private Set<AuthRole> roles;
	private long lastAccessed;
	private int websocketConnections;
	
	/**
	 * Constructor for node principal.
	 * @param nodeId unique node client id
	 * @param clientDn distinguished name. Client information a la X.509/LDAP/etc. Should contain at least CN=display name
	 */
	public Principal(int nodeId, AuthInfo info){
		this(info);
		this.nodeId = nodeId;
	}

	private Principal(AuthInfo info) {
		this.clientDn = info.getClientDN();
		this.roles = info.getRoles();
		this.websocketConnections = 0;
		// TODO load client DN correctly
		if( clientDn != null && clientDn.startsWith("CN=") ){
			int e = clientDn.indexOf(',');
			if( e == -1 ){
				// only one component, use the remaining string
				e = clientDn.length();
			}
			commonName = clientDn.substring(3, e);
		}		
	}

	static Principal createAdminPrincipal(AuthInfo info) {
		Principal p = new Principal(info);
		p.nodeId = null;
		return p;
	}

	/**
	 * Determine whether the principal is a client node. For client nodes, {@link #getNodeId()} will be defined.
	 * @return {@code true} if the principal is a node.
	 */
	public boolean isNode() {
		return this.nodeId != null;
	}
	/**
	 * Retrieve the full client DN string
	 * @return distinguished name
	 */
	public String getClientDN(){
		return clientDn;
	}

	/**
	 * Retrieve the user name
	 */
	@Override
	public String getName() {
		if( commonName != null ){
			// return client CN
			return commonName;
		}else{
			// use node id for common name
			return Integer.toString(getNodeId());
		}
	}
	/**
	 * Get the unique node client id. 
	 * Will throw {@link UnsupportedOperationException} if the principal is not a node
	 * @return client id
	 * @throws UnsupportedOperationException if the principal is not a node
	 */
	public int getNodeId() throws UnsupportedOperationException{
		if( nodeId == null ) {
			throw new UnsupportedOperationException("Principal is not a node: "+clientDn);
		}
		return nodeId;
	}
	@Override
	public java.security.Principal getUserPrincipal() {
		return this;
	}

	/**
	 * This method is not used right now.
	 */
	@Override
	public boolean isUserInRole(String role) {
		try {
			return roles.contains(AuthRole.valueOf(role));
		}catch( IllegalArgumentException e ) {
			return false;
		}
	}
	@Override
	public boolean isSecure() {
		return true;
	}
	@Override
	public String getAuthenticationScheme() {
		return SecurityContext.CLIENT_CERT_AUTH;
	}
	public boolean isAdmin(){
		return roles.contains(AuthRole.ADMIN_READ);
	}

	/**
	 * Update the last contact / last access timestamp for this user
	 */
	public void updateLastAccessed(){
		this.lastAccessed = System.currentTimeMillis();
	}
	/**
	 * Get the timestamp for the last known contact to the principal.
	 * @return epoch millis, e.g. similar to what is returned {@link System#currentTimeMillis()}
	 */
	public long getLastAccessed(){
		return this.lastAccessed;
	}

	public void incrementWebsocketCount() {
		this.websocketConnections ++;
	}
	public void decrementWebsocketCount() {
		this.websocketConnections --;
	}
	public int getWebsocketCount() {
		return this.websocketConnections;
	}

	@Override
	public String toString() {
		return "Principal(nodeId="+nodeId+", dn="+this.clientDn+")";
	}
}
