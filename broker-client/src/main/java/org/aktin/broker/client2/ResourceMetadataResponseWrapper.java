package org.aktin.broker.client2;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;

import org.aktin.broker.client.ResourceMetadata;

public class ResourceMetadataResponseWrapper implements ResourceMetadata {
	private String name;
	private HttpResponse<InputStream> resp;

	public ResourceMetadataResponseWrapper(String name, HttpResponse<InputStream> resp) {
		this.name = name;
		this.resp = resp;
	}
	@Override
	public long getLastModified() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte[] getMD5() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMD5String() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return resp.body();
	}

}
