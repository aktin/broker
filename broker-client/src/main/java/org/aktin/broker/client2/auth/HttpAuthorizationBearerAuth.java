package org.aktin.broker.client2.auth;

import java.io.IOException;
import java.net.http.WebSocket.Builder;

import org.aktin.broker.client2.AuthFilter;

public abstract class HttpAuthorizationBearerAuth implements AuthFilter{

	protected abstract String getBearerToken() throws IOException;

	@Override
	public void addAuthentication(Builder builder) throws IOException {
		builder.header("Authorization", "Bearer "+getBearerToken());
	}

	@Override
	public void addAuthentication(java.net.http.HttpRequest.Builder builder) throws IOException {
		builder.header("Authorization", "Bearer "+getBearerToken());
	}

}
