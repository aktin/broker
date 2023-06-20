package org.aktin.broker.client2.auth;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient.Builder;

import lombok.extern.java.Log;

/**
 * API key authentication with a user/password protected proxy.
 * Use with care. Sending passwords to proxies is usually considered insecure.
 * Set system properties custom.proxyHost, custom.proxyPort, custom.proxyUser, custom.proxyPassword
 * 
 * @author R.W.Majeed
 *
 */
@Log
public class ApiKeyAuthenticationWithProxyPassword extends ApiKeyAuthentication{
	private Authenticator proxyAuth;
	private String proxyHost;
	private int proxyPort;
	private String proxyUser;
	private char[] proxyPassword;

	public ApiKeyAuthenticationWithProxyPassword(String apiKey) {
		super(apiKey);
		this.proxyHost = System.getProperty("custom.proxyHost","");
		this.proxyPort = Integer.parseInt(System.getProperty("custom.proxyPort","8080"));
		this.proxyUser = System.getProperty("custom.proxyUser");
		this.proxyPassword = System.getProperty("custom.proxyPassword","").toCharArray();

		log.info("Using custom HTTP proxy authentication");
		log.info("Proxy host: "+proxyHost);
		log.info("Proxy port: "+proxyPort);
		log.info("Proxy user: "+proxyUser);
		log.info("Proxy password of length "+proxyPassword.length);

		this.proxyAuth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				if (getRequestorType() != Authenticator.RequestorType.PROXY) {
					// we will only answer auth requests by proxy
					return null;
				}
				if( !proxyHost.equalsIgnoreCase(getRequestingHost()) 
						|| !(getRequestingPort() == proxyPort) ) 
				{
					// not our proxy. don't give any authentication
					return null;
				}
				// method, host and port are matching, give user and password
				return new PasswordAuthentication(proxyUser, proxyPassword);
			}
		};
	}

	@Override
	public void configureHttpClient(Builder builder) throws IOException {
		super.configureHttpClient(builder);
        builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, proxyPort)));
        builder.authenticator(this.proxyAuth);
	}

}
