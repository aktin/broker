package org.aktin.broker.auth;

import javax.ws.rs.core.SecurityContext;

public class Principal implements java.security.Principal, SecurityContext{

	private int nodeId;
	private String commonName;
	private String clientDn;
	private long lastAccessed;
	
	public Principal(int nodeId, String clientDn){
		this.nodeId = nodeId;
		this.clientDn = clientDn;
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

	public String getClientDN(){
		return clientDn;
	}
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
	public int getNodeId(){
		return nodeId;
	}
	@Override
	public java.security.Principal getUserPrincipal() {
		return this;
	}
	@Override
	public boolean isUserInRole(String role) {
		return role.equals("admini");
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

	public void updateLastAccessed(){
		this.lastAccessed = System.currentTimeMillis();
	}
	public long getLastAccessed(){
		return this.lastAccessed;
	}
}
