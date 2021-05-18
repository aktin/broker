package org.aktin.broker.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.WebSocketListener;

public class ClientWebsocket implements WebSocketListener {
	public CountDownLatch expectedMessages;
	public List<String> messages;
	
	public ClientWebsocket() {
		messages = new ArrayList<>();
	}

	public void prepareForMessages(int count){
		expectedMessages = new CountDownLatch(count);
	}
	public void waitForMessages(long delay, TimeUnit unit) throws InterruptedException{
		expectedMessages.await(delay, unit);
	}
	@Override
	public void onWebSocketBinary(byte[] payload, int offset, int len) {
	}
	@Override
	public void onWebSocketClose(int statusCode, String reason) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onWebSocketConnect(org.eclipse.jetty.websocket.api.Session session) {
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
		messages.add(message);
		if( expectedMessages != null ){
			expectedMessages.countDown();
		}
	}
}
