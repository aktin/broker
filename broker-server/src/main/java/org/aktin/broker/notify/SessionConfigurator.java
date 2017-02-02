package org.aktin.broker.notify;

import java.util.Collections;

import javax.inject.Inject;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.aktin.broker.Authenticated;
import org.aktin.broker.auth.AuthCache;
import org.aktin.broker.auth.HeaderAuthentication;

public class SessionConfigurator extends javax.websocket.server.ServerEndpointConfig.Configurator {

	@Inject
	HeaderAuthentication auth;
	
	@Inject
	AuthCache cache;
	
	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		// can check header here
		
		if( false ){
			// abort handshake by sending empty accept
			auth.authenticateByHeaders(request.getHeaders());
		    response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, Collections.emptyList());
		}else{
			super.modifyHandshake(sec, request, response);
		}
	}

}
