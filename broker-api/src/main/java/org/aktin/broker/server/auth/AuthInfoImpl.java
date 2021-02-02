package org.aktin.broker.server.auth;

import java.util.Set;

public class AuthInfoImpl implements AuthInfo {
	private String userId;
	private String clientDN;
	private Set<AuthRole> roles;

	public AuthInfoImpl(String userId, String clientDN, Set<AuthRole> roles) {
		this.userId = userId;
		this.clientDN = clientDN;
		this.roles = roles;
	}

	@Override
	public String getUserId() {
		return userId;
	}

	@Override
	public String getClientDN() {
		return clientDN;
	}

	@Override
	public Set<AuthRole> getRoles() {
		return roles;
	}

}
