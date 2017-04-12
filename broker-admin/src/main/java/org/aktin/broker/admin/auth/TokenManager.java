package org.aktin.broker.admin.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Singleton;

@Singleton
public class TokenManager {
	private static final Logger log = Logger.getLogger(TokenManager.class.getName());
	private Map<String,Token> map;
	private String simplePassword;

	public TokenManager(){
		this.map = new HashMap<>();
		simplePassword = System.getProperty("aktin.broker.password");
		if( simplePassword == null ){
			// generate random password
			StringBuilder b = new StringBuilder(8);
			for( int i=0; i<b.capacity(); i++ ){
				b.append(Character.valueOf((char)('0'+Math.random()*('Z'-'0'))));
			}
			simplePassword = b.toString();
		}
		// TODO use normal logging or even better real password management
		System.err.println("Using password: "+simplePassword);
		log.info("Using password: "+simplePassword);
	}
	/**
	 * Authenticate user and register token
	 * @param username name
	 * @param charArray password
	 * @return token
	 */
	Token authenticate(String username, char[] charArray){
		// XXX for rapid prototyping, use system property for password
		if( !simplePassword.equals(new String(charArray)) ){
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
