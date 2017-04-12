package org.aktin.broker.admin.auth;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

@Singleton
public class TokenManager {
	private Map<String,Token> map;

	public TokenManager(){
		this.map = new HashMap<>();
	}
	/**
	 * Authenticate user and register token
	 * @param username name
	 * @param charArray password
	 * @return token
	 */
	Token authenticate(String username, char[] charArray){
		// XXX for rapid prototyping, use system property for password
		String pw = System.getProperty("áktin.broker.password","");
		if( !pw.equals(new String(charArray)) ){
			// invalid password, fail authentication by returning null token
			return null;
		}
		Token t = new Token(username);
		map.put(t.getGUID(), t);
		return t;
	}

	public Token lookupToken(String guid){
		return map.get(guid);
	}
}
