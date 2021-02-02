package org.aktin.broker.auth.cred;

import java.security.Principal;

public class Token implements Principal{
	private String user;
	private long issued;

	public Token(String user){
		this.user = user;
		this.issued = System.currentTimeMillis();
	}
	public String getGUID(){
		return Long.toHexString(System.identityHashCode(this)*this.issued);
	}

	public void invalidate() {
		// TODO Auto-generated method stub
		
	}

	public long issuedTimeMillis() {
		return issued;
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
