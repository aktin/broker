package org.aktin.broker.client2;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.WebSocket;

/**
 * Interface for client authentication
 * @author R.W.Majeed
 *
 */
public interface AuthFilter {
	void addAuthentication(WebSocket.Builder builder) throws IOException;
	void addAuthentication(HttpRequest.Builder builder) throws IOException;
	/**
	 * Configure the HTTP client. The client is usually configured only once
	 * and reused for all connection.
	 * @param builder HTTP client builder
	 * @throws IOException IO error
	 */
	default void configureHttpClient(HttpClient.Builder builder) throws IOException {};
}
