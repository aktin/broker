package org.aktin.broker.admin.auth;

import java.security.Principal;

public class Token implements Principal{
	private String user;

	public Token(String user){
		this.user = user;
	}
	public String getGUID(){
		return Integer.toString(System.identityHashCode(this));
	}

	public void invalidate() {
		// TODO Auto-generated method stub
		
	}

	public long issuedTimeMillis() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		return user;
	}

	public boolean isAdmin() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasRole(String role) {
		// TODO Auto-generated method stub
		return false;
	}

	public void renew() {
		// TODO Auto-generated method stub
		
	}
}
