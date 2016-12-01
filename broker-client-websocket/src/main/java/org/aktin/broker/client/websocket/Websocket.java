package org.aktin.broker.client.websocket;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public abstract class Websocket extends WebSocketClient{

	public Websocket(URI serverURI) {
		super(serverURI);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessage(String message) {
		String[] parts = message.split(" ");
		switch( parts[0] ){
		case "published":
			onRequestPublished(parts[1]);
			break;
		case "closed":
			onRequestClosed(parts[1]);
			break;
		case "status":
			onRequestNodeStatus(parts[1], Integer.parseInt(parts[2]), parts[3]);
			break;
		}
	}

	public abstract void onRequestPublished(String requestId);
	public abstract void onRequestClosed(String requestId);
	public abstract void onRequestNodeStatus(String requestId, int node, String status);
	
	@Override
	public void onClose(int code, String reason, boolean remote) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(Exception ex) {
		// TODO Auto-generated method stub
		
	}

}
