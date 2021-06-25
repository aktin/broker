package org.aktin.broker.client2;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

/**
 * Websocket notification service. Ensures that text messages are complete before 
 * calling the methods {@link #notifyClose(int)} and {@link #notifyText(String)} asynchronously
 * with the executor specified in the constructor.
 * 
 * @author R.W.Majeed
 *
 */
public abstract class WebsocketNotificationService implements WebSocket.Listener{

	private ExecutorService executor;
	private StringBuilder text; // collect incomplete parts

	public WebsocketNotificationService(ExecutorService executor) {
		this.executor = executor;
		this.text = new StringBuilder();
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		text.append(data);
		if( last == true ) {
			// process completed message
			String message = text.toString();
			text = new StringBuilder();
			executor.execute( () -> notifyText(message) );
		}else {
			// at a certain load, java will provide incomplete messages
			;// we don't need to do anything here
		}
		webSocket.request(1);
		return null; // reclaim CharSequence immediately
	}
	
	protected abstract void notifyClose(int statusCode);

	/**
	 * Handle notification of a completed text message.
	 * @param text message contents
	 */
	protected abstract void notifyText(String text);
	
	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		executor.execute( () -> notifyClose(statusCode) );
		return null; // websocket can be closed immediately
	}

	/**
	 * Shuts down the executor
	 */
	public void shutdown() {
		executor.shutdownNow();
	}
}
