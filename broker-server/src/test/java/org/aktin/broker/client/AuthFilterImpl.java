package org.aktin.broker.client;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.WebSocket;
import java.util.function.BiConsumer;

import org.aktin.broker.client2.AuthFilter;
import org.aktin.broker.util.AuthFilterSSLHeaders;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AuthFilterImpl implements AuthFilter{
	private String certId;
	private String clientDn;

	private final void setHeaders(BiConsumer<String, String> setter) {
		setter.accept(AuthFilterSSLHeaders.X_SSL_CLIENT_ID, certId);
		setter.accept(AuthFilterSSLHeaders.X_SSL_CLIENT_DN, clientDn);
		setter.accept(AuthFilterSSLHeaders.X_SSL_CLIENT_VERIFY, "SUCCESS");
	}
	@Override
	public void addAuthentication(WebSocket.Builder builder) throws IOException {
		setHeaders(builder::header);
	}

	@Override
	public void addAuthentication(HttpRequest.Builder builder) throws IOException {
		setHeaders(builder::header);
	}

}
