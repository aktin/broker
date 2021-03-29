package org.aktin.broker.client2;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public abstract class WebsocketNotificationService implements WebSocket.Listener{

	private ExecutorService executor;

	public WebsocketNotificationService(ExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		if( last == true ) {
			webSocket.request(1);
			return CompletableFuture.runAsync( () -> notifyText(data.toString()), executor);			
		}else {
			return CompletableFuture.failedStage(new IOException("Partial websocket message not supported"));
		}
	}
	
	protected abstract void notifyClose(int statusCode);

	protected abstract void notifyText(String text);
	
	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		executor.execute( () -> notifyClose(statusCode) );
		return null; // websocket can be closed immediately
	}

	public void shutdown() {
		executor.shutdownNow();
	}
}
