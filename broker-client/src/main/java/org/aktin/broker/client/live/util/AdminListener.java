package org.aktin.broker.client.live.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aktin.broker.client2.AdminNotificationListener;
import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.client2.BrokerAdmin2;

import lombok.AllArgsConstructor;

public class AdminListener implements AdminNotificationListener, Runnable{
	private BrokerAdmin2 admin;
	private AtomicBoolean abort;
	private WebSocket websocket;

    @AllArgsConstructor
    private static class ApiKeyAuthFilter implements AuthFilter{
    	private String key;
		@Override
		public void addAuthentication(WebSocket.Builder builder) throws IOException {
			builder.header("Authorization", "Bearer "+key);
		}

		@Override
		public void addAuthentication(java.net.http.HttpRequest.Builder builder) throws IOException {
			builder.header("Authorization", "Bearer "+key);
		}
    }

	public AdminListener(BrokerAdmin2 admin) throws IOException{
		this.admin = admin;
		this.abort = new AtomicBoolean();
		this.websocket = admin.openWebsocket(this);
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				AdminListener.this.abort.set(true);
				AdminListener.this.notifyAll();
			}
		});
	}

	public static void main(String[] args) {
		if( args.length != 2 ) {
			System.err.println("Usage: "+AdminListener.class.getPackageName()+"."+AdminListener.class.getName()+" <BROKER_ENDPOINT_URI> <APIKEY>");
			return;
		}
		BrokerAdmin2 admin = new BrokerAdmin2(URI.create(args[0]));
		admin.setAuthFilter(new ApiKeyAuthFilter(args[1]));
		try {
			new AdminListener(admin).run();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println();
			System.err.println("Failed to initialize: "+e.getMessage());
		}
	}

	@Override
	public void onRequestCreated(int requestId) {
		System.out.println("request created "+requestId);
	}

	@Override
	public void onRequestPublished(int requestId) {
		System.out.println("request published "+requestId);
	}

	@Override
	public void onRequestClosed(int requestId) {
		System.out.println("request closed "+requestId);
	}

	@Override
	public void onRequestStatusUpdate(int requestId, int nodeId, String status) {
		System.out.println("request status "+requestId+" "+nodeId+" "+status);
	}

	@Override
	public void onRequestResultUpdate(int requestId, int nodeId, String mediaType) {
		System.out.println("request result "+requestId+" "+nodeId+" "+mediaType);
	}

	@Override
	public void onResourceUpdate(int nodeId, String resourceId) {
		System.out.println("resource update "+nodeId+" "+resourceId);
	}

	@Override
	public void run() {
		while( abort.get() == false ) {
			synchronized( this ) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					;// abort or other interrupt
				}
			}
			if( websocket.isInputClosed() ) {
				// try to reconnect
				try {
					websocket = admin.openWebsocket(this);
					System.out.println("websocket reconnected");
				} catch (IOException e) {
					System.err.println("websocket reconnect failed: "+e.getMessage());
					// abort
					abort.set(true);
				} 
			}
		}
		websocket.abort();
	}

	@Override
	public void onWebsocketClosed(int statusCode, String reason) {
		System.out.println("websocket closed with status "+statusCode+":"+reason);
		this.notifyAll();
	}

}
