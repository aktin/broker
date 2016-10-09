package org.aktin.broker.auth;

import javax.ws.rs.core.SecurityContext;

public class Principal implements java.security.Principal, SecurityContext{

	private int nodeId;
	
	public Principal(int nodeId, String clientDn){
		this.nodeId = nodeId;
		// TODO load client DN
	}
	@Override
	public String getName() {
		// TODO return client CN
		return Integer.toString(getNodeId());
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
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isSecure() {
		return true;
	}
	@Override
	public String getAuthenticationScheme() {
		return SecurityContext.CLIENT_CERT_AUTH;
	}

}
