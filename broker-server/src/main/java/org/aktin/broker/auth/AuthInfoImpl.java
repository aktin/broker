package org.aktin.broker.auth;

import java.util.Set;

import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthRole;

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
