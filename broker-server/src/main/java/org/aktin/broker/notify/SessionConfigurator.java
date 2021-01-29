package org.aktin.broker.notify;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import javax.inject.Inject;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.aktin.broker.auth.HeaderAuthentication;
import org.aktin.broker.auth.Principal;

public class SessionConfigurator extends javax.websocket.server.ServerEndpointConfig.Configurator {
	public static final String AUTH_USER = "auth-user";

	@Inject
	HeaderAuthentication auth;

	private static final Function<String,String> mapSingleHeader(Map<String,List<String>> headers){
		return new Function<String, String>() {

			@Override
			public String apply(String name) {
				List<String> h = headers.get(name);
				if( h == null || h.size() == 0 ) {
					return null;
				}else {
					return h.get(1);
				}
			}
		};
	}
	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		Objects.requireNonNull(auth, "CDI failed for HeaderAuthentication");
		// can check header here
		Principal user = null;
		try{
			user = auth.authenticateByHeaders(mapSingleHeader(request.getHeaders()));
		}catch( IOException e ) {
			// TODO log error
		}
		if( user != null ) {
			sec.getUserProperties().put(AUTH_USER,user);
			super.modifyHandshake(sec, request, response);
		}else {
			// abort handshake by sending empty accept
			response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, Collections.emptyList());
		}
	}

}
