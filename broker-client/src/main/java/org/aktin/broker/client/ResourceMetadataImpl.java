package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Base64;

import javax.activation.DataSource;

public class ResourceMetadataImpl implements DataSource, ResourceMetadata{
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
		StringBuilder b = new StringBuilder(16*2);
		byte[] md5 = getMD5();
		for( int i=0; i<md5.length; i++ ){
			String hex = Integer.toHexString(Byte.toUnsignedInt(md5[i]));
			if( hex.length() == 1 ){
				// prefix with 0
				b.append('0');
			}
			b.append(hex);
		}
		return b.toString();
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
