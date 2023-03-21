package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.time.Instant;

import jakarta.activation.DataSource;

public class URLConnectionDataSource implements DataSource{
	private URLConnection connection;
	
	public URLConnectionDataSource(URLConnection connection) {
		this.connection = connection;
	}
	@Override
	public String getContentType() {
		return connection.getContentType();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return connection.getInputStream();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

	public Instant getLastModified(){
		long ts = connection.getHeaderFieldDate("last-modified", 0);
		if( ts == 0 ){
			return null;
		}else{
			return Instant.ofEpochMilli(ts);
		}
	}
	

}
