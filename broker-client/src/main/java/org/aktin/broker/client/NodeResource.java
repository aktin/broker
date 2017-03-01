package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

public class NodeResource extends ResourceMetadata {

	public NodeResource(HttpURLConnection c, String name){
		super(c, name);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return c.getInputStream();
	}
}
