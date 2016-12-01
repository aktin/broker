package org.aktin.broker.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jetty.websocket.api.WebSocketListener;

public class ClientWebsocket implements WebSocketListener {
	public CountDownLatch expectedMessages;
	public List<String> messages;
	
	public ClientWebsocket() {
		messages = new ArrayList<>();
		expectedMessages = new CountDownLatch(1);
	}
	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
		System.out.println("Client websocket closed");
	}
	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onWebSocketConnect(org.eclipse.jetty.websocket.api.Session session) {
		System.out.println("Client websocket open");
		try {
			session.getRemote().sendString("Hallo vom Client");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
	@Override
	public void onWebSocketError(Throwable cause) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onWebSocketText(String message) {
		System.out.println("Client received message: "+message);
		messages.add(message);
		expectedMessages.countDown();
	}
}
