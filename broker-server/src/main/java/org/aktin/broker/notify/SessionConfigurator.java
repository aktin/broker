package org.aktin.broker.notify;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.aktin.broker.auth.HeaderAuthentication;
import org.aktin.broker.auth.Principal;

/**
 * Websocket session configurator with authentication. Performs authentication
 * during websocket handshake. If successfull, authenticated user {@link Principal}
 * instance is stored in the websocket {@link Session#getUserProperties()} with
 * the key {@link #AUTH_USER}.
 *
 * @author R.W.Majeed
 *
 */
public class SessionConfigurator extends javax.websocket.server.ServerEndpointConfig.Configurator {
	private static final Logger log = Logger.getLogger(SessionConfigurator.class.getName());
	/**
	 * Websocket session user properties key where the authenticated user {@link Principal} is stored.
	 */
	public static final String AUTH_USER = "auth-user";

	private HeaderAuthentication auth;

	@Inject
	public SessionConfigurator(HeaderAuthentication auth) {
		this.auth = auth;
	}
	/**
	 * Reduce multiple HTTP headers per key to only one header by key
	 * @param headers HTTP headers with multiple headers
	 * @return reduced list with only first header per key used
	 */
	private static final Function<String,String> mapSingleHeader(Map<String,List<String>> headers){
		return new Function<String, String>() {

			@Override
			public String apply(String name) {
				List<String> h = headers.get(name);
				if( h == null || h.size() == 0 ) {
					return null;
				}else {
					return h.get(0);
				}
			}
		};
	}
	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		// make sure authentication interface is available
		Objects.requireNonNull(auth, "CDI failed for HeaderAuthentication");

		// can check header here
		Principal user = null;
		try{
			user = auth.authenticateByHeaders(mapSingleHeader(request.getHeaders()));
		}catch( IOException e ) {
			log.log(Level.WARNING, "Unexpected authentication failure", e);
		}
		super.modifyHandshake(sec, request, response);
		if( user != null ) {
			log.info("Websocket handshake auth user: "+user.getName());
			sec.getUserProperties().put(AUTH_USER,user);
		}else {
			log.info("Websocket handshake auth unauthorized. Aborting handshake.");
			// abort handshake by sending empty accept
			response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, Collections.emptyList());
		}
	}

}
