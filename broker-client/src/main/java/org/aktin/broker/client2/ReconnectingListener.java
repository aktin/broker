package org.aktin.broker.client2;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.logging.Level;

import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

@AllArgsConstructor
@Log
public class ReconnectingListener implements NotificationListener{
	private AbstractBrokerClient<?> client;
	private int websocketReconnectMillis;
	/**
	 * Maximum number of retries. For unlimited retries, set to {@code -1} 
	 */
	private int maxRetries;


	private static final class ReconnectAdmin extends ReconnectingListener implements AdminNotificationListener{
		public ReconnectAdmin(AbstractBrokerClient<?> client, int websocketReconnectMillis, int maxRetries) {
			super(client, websocketReconnectMillis, maxRetries);
		}
		@Override
		public void onRequestCreated(int requestId) {}
		@Override
		public void onRequestPublished(int requestId) {}
		@Override
		public void onRequestClosed(int requestId) {}
		@Override
		public void onRequestStatusUpdate(int requestId, int nodeId, String status) {}
		@Override
		public void onRequestResultUpdate(int requestId, int nodeId, String mediaType) {}
		@Override
		public void onResourceUpdate(int nodeId, String resourceId) {}
	}
	private static final class ReconnectClient extends ReconnectingListener implements ClientNotificationListener{
		public ReconnectClient(AbstractBrokerClient<?> client, int websocketReconnectMillis, int maxRetries) {
			super(client, websocketReconnectMillis, maxRetries);
		}
		@Override
		public void onRequestPublished(int requestId) {}
		@Override
		public void onRequestClosed(int requestId) {}
		@Override
		public void onResourceChanged(String resourceName) {}
		@Override
		public void onPong(String msg) {}
	}

	public static final AdminNotificationListener forAdmin(BrokerAdmin2 admin, int reconnectMillis, int maxRetries) {
		return new ReconnectAdmin(admin, reconnectMillis, maxRetries);
	}
	public static final ClientNotificationListener forClient(BrokerClient2 client, int reconnectMillis, int maxRetries) {
		return new ReconnectClient(client, reconnectMillis, maxRetries);
	}
	/**
	 * Called when a reconnection attempt for the websocket failed. The failure count is reset after a successful connection, otherwise it is increased.
	 * @param failCount successive reconnection failures. This number will be always at least 1, since this method will be called only after the immediate reconneciton attempt failed.
	 * @return {@code true} to continue attempting to reconnect. False to stop reconnecting
	 */
	protected boolean onWebsocketReconnectFailed(int failCount) {
		if( maxRetries != -1 && failCount > maxRetries) {
			return false;
		}
		// connection failed, try again after delay
		// note that we are in a separate thread provided by the AKTIN client library for websocket callbacks
		// therefore we can block here without breaking anything else
		log.log(Level.INFO, "Waiting for next try to re-connect websocket in {0}ms", websocketReconnectMillis);
		try {
			Thread.sleep(websocketReconnectMillis);
		} catch (InterruptedException e) {
			// interruption can be early, but we don't care to connect earlier
		}
		return true;
	}
	@Override
	public void onWebsocketClosed(int statusCode) {
		// try to reconnect
		WebSocket ws = client.getWebsocket();
		int failCount = 0;
		boolean retry = true;
		while( ws == null && retry == true ) {
			try {
				client.connectWebsocket();
				log.info("Websocket connection re-established.");
				// connection successful.
			}catch( IOException e ) {
				// unable to connect
				log.log(Level.WARNING, "Unable to reconnect closed websocket in attempt {0}: {1}",new Object[] {failCount, e}); 
			}
			// try to retrieve the connected websocket
			ws = client.getWebsocket();
			if( ws == null ) {
				failCount ++;
				retry = onWebsocketReconnectFailed(failCount);
			}
		}
		if( ws == null ) {
			log.warning("Stopping reconnection attempts");
		}
	}


}
