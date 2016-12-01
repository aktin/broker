package org.aktin.broker.notify;

import java.util.Collections;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class SessionConfigurator extends javax.websocket.server.ServerEndpointConfig.Configurator {
	
	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
		// can check header here
		
		if( false ){
			// abort handshake by sending empty accept
		    response.getHeaders().put(HandshakeResponse.SEC_WEBSOCKET_ACCEPT, Collections.emptyList());
		}else{
			super.modifyHandshake(sec, request, response);
		}
	}

}
