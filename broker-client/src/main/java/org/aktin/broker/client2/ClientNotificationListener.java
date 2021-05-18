package org.aktin.broker.client2;

public interface ClientNotificationListener extends NotificationListener {
	void onRequestPublished(int requestId);
	void onRequestClosed(int requestId);
	void onResourceChanged(String resourceName);
}
