package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Base64;

import jakarta.activation.DataSource;

public class ResourceMetadataImpl implements DataSource, ResponseWithMetadata{
	protected HttpURLConnection c;
	protected String name;

	public ResourceMetadataImpl(HttpURLConnection c, String name){
		this.c = c;
		this.name = name;
	}
	@Override
	public long getLastModified(){
		return c.getLastModified();
	}
	@Override
	public byte[] getMD5(){
		String md5 = c.getHeaderField("Content-MD5");
		if( md5 != null ){
			return Base64.getDecoder().decode(md5);
		}else{
			return null;
		}

	}
	@Override
	public String getMD5String(){
		return Utils.toHexString(getMD5());
	}
	@Override
	public String getContentType() {
		return c.getContentType();
	}

	@Override
	public String getName() {
		return name;
	}
	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}
	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}
}
