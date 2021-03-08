package org.aktin.broker.client2;

public interface AdminNotificationListener {
	void onRequestCreated(int requestId);
	void onRequestPublished(int requestId);
	void onRequestClosed(int requestId);
	void onRequestStatusUpdate(int requestId, int nodeId, String status);
	void onRequestResultUpdate(int requestId, int nodeId, String mediaType);
	void onResourceUpdate(int nodeId, String resourceId);
	void onWebsocketClosed(int statusCode, String reason);
}
