package org.aktin.broker.client2;

import java.io.IOException;
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
}
