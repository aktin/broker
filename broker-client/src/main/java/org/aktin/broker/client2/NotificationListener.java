package org.aktin.broker.client2;

public interface NotificationListener {
	void onWebsocketClosed(int statusCode);
	default void onPong(String msg) {};
}
