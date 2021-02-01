package org.aktin.broker.server.auth;

import java.util.Set;

public interface AuthInfo {
	/**
	 * User ID is unique and should not change
	 * @return user id
	 */
	String getUserId();
	
	/**
	 * Distinguished name. Client information a la X.509/LDAP/etc. Should contain at least CN=display name
	 * @return client DN
	 */
	String getClientDN();


	Set<AuthRole> getRoles();

}
