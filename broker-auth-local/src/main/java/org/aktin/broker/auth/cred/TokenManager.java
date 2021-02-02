package org.aktin.broker.auth.cred;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import javax.inject.Singleton;

/**
 * Simple password based token manager. Currently, only a single (admin) user
 * with one password is supported.
 *
 * @author R.W.Majeed
 *
 */
@Singleton
public class TokenManager {
	public static final String PROPERTY_BROKER_PASSWORD = "aktin.broker.password"; 
	private static final Logger log = Logger.getLogger(TokenManager.class.getName());
	private Map<String,Token> map;
	private BiFunction<String, String, Boolean> authenticator;

	public TokenManager(final String simplePassword) {
		this.authenticator = (login,password) -> password.contentEquals(simplePassword);
		this.map = new HashMap<>();
	}
	public TokenManager(){
		final String simplePassword = System.getProperty(PROPERTY_BROKER_PASSWORD, randomPassword());
		// TODO use normal logging or even better real password management
		System.err.println("Using password: "+simplePassword);
		log.info("Using password: "+simplePassword);
		this.authenticator = (login,password) -> password.contentEquals(simplePassword);
	}
	
	public static final String randomPassword() {
		StringBuilder b = new StringBuilder(8);
		for( int i=0; i<b.capacity(); i++ ){
			b.append(Character.valueOf((char)('0'+Math.random()*('z'-'0'))));
		}
		return b.toString();
	}

	/**
	 * Authenticate user and register token
	 * @param username name
	 * @param charArray password
	 * @return token
	 */
	Token authenticate(String username, char[] charArray){
		// XXX for rapid prototyping, use system property for password
		if( !authenticator.apply(username, new String(charArray)) ){
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
