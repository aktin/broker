package org.aktin.broker.client.live.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.WebSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aktin.broker.client.live.BrokerConfiguration;
import org.aktin.broker.client2.AdminNotificationListener;
import org.aktin.broker.client2.BrokerAdmin2;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Standalone program which connects to a broker with admin privileges and listens
 * to any changes or updates via websocket. All updates are printed to {@code stdout}.
 *
 * Arguments to {@link #main(String[])}: broker endpoint uri, auth filter implementation class, authentication argument.
 * e.g. {@code http://localhost:8080/broker/ org.aktin.broker.client2.auth.ApiKeyAuthentication xxxAdmin1234}
 *
 * @author R.W.Majeed
 *
 */
public class AdminListener implements AdminNotificationListener, Runnable{
	private BrokerAdmin2 admin;
	private AtomicBoolean abort;
	private WebSocket websocket;

	public AdminListener(BrokerAdmin2 admin) throws IOException{
		this.admin = admin;
		this.abort = new AtomicBoolean();
		admin.addListener(this);
		this.websocket = admin.connectWebsocket();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				AdminListener.this.abort.set(true);
				AdminListener.this.notifyAll();
			}
		});
	}

	@Getter
	@AllArgsConstructor
	public static class Config implements BrokerConfiguration{
		private URI brokerEndpointURI;
		private String authClass;
		private String authParam;		
	}
	public static void main(String[] args) {
		if( args.length != 3 ) {
			System.err.println("Usage: "+AdminListener.class.getPackageName()+"."+AdminListener.class.getName()+" <BROKER_ENDPOINT_URI> <AuthFilterImplementationClass> <AuthFilterArgument>");
			System.err.println("e.g.: "+AdminListener.class.getPackageName()+"."+AdminListener.class.getName()+" http://localhost:8080/broker/ org.aktin.broker.client2.auth.ApiKeyAuthentication xxxAdmin1234");			
			return;
		}
		Config config = new Config(URI.create(args[0]), args[1], args[2]);
		BrokerAdmin2 admin = new BrokerAdmin2(config.getBrokerEndpointURI());
		admin.setAuthFilter(config.instantiateAuthFilter());
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
					websocket = admin.connectWebsocket();
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
	public void onWebsocketClosed(int statusCode) {
		System.out.println("websocket closed with status "+statusCode);
		this.notifyAll();
	}

}
