package org.aktin.broker.auth;

import javax.ws.rs.core.SecurityContext;

public class Principal implements java.security.Principal, SecurityContext{

	private int nodeId;
	private String commonName;
	private String clientDn;
	private long lastAccessed;
	private int websocketConnections;
	
	/**
	 * Constructor for principal.
	 * @param nodeId unique node client id
	 * @param clientDn distinguished name. Client information a la X.509/LDAP/etc. Should contain at least CN=display name
	 */
	public Principal(int nodeId, String clientDn){
		this.nodeId = nodeId;
		this.clientDn = clientDn;
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
	 * @return client id
	 */
	public int getNodeId(){
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
		return role.equals("admin");
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
		return isAdminDN(clientDn);
	}
	public static final boolean isAdminDN(String clientDn){
		// TODO correct parsing/handling of DN
		return clientDn != null && clientDn.contains("OU=admin");		
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
}
