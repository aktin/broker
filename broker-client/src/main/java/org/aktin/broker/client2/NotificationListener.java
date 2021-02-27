package org.aktin.broker.client2;

public interface NotificationListener {
	void onRequestPublished(int requestId);
	void onRequestClosed(int requestId);
	void onResourceChanged(String resourceName);
	void onWebsocketClosed(int statusCode, String reason);
}
