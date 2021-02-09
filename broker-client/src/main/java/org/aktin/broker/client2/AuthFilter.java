package org.aktin.broker.client2;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.WebSocket;

public interface AuthFilter {
	void addAuthentication(WebSocket.Builder builder) throws IOException;
	void addAuthentication(HttpRequest.Builder builder) throws IOException;	
}
