package org.aktin.broker.client.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * Authentication via SSL/TLS client certificates.
 * This class is depreacted. Use BrokerClient2/BrokerAdmin2
 * @author R.W.Majeed
 *
 */
@Deprecated
public class TlsClientCertAuthenticator implements ClientAuthenticator {

	private SSLContext ssl;

	public TlsClientCertAuthenticator(KeyStore keystore, char[] password) throws KeyManagementException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException{
		initialiseSSLContext(keystore, password);
	}
	public TlsClientCertAuthenticator(InputStream keystore, char[] password) throws IOException, GeneralSecurityException{
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(keystore, password);
		initialiseSSLContext(ks, password);
	}
	private void initialiseSSLContext(KeyStore keystore, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, KeyManagementException{
		// Algorithm should be SunX509
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keystore, password);
		SSLContext sc = SSLContext.getInstance("TLS");
		// TODO use trust factory?
		sc.init(kmf.getKeyManagers(), null, null);		
	}
	@Override
	public HttpURLConnection openAuthenticatedConnection(URL url) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// make sure we can set the SSL socket factory
		if( !(connection instanceof HttpsURLConnection) ){
			throw new IOException("No HTTPS URL: "+url);
		}
		// set the socket factory
	    ((HttpsURLConnection)connection).setSSLSocketFactory(ssl.getSocketFactory());
	    // authentication will be performed transparently via SSL
	    return connection;
	}

}
