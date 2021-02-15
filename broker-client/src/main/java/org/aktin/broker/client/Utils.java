package org.aktin.broker.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class Utils {

	public static BufferedReader contentReaderForInputStream(InputStream in, String contentType, Charset defaultCharset) throws UnsupportedEncodingException, IOException{
		// default HTTP charset
		Charset charset = defaultCharset;
		// use charset from content-type header
		if( contentType != null ) {
			int csi = contentType.indexOf("charset=");
			if( csi != -1 ){
				charset = Charset.forName(contentType.substring(csi+8));
			}			
		}
		return new BufferedReader(new InputStreamReader(in, charset));
	}

	public static final String toHexString(byte[] bytes) {
		StringBuilder b = new StringBuilder(16*2);
		for( int i=0; i<bytes.length; i++ ){
			String hex = Integer.toHexString(Byte.toUnsignedInt(bytes[i]));
			if( hex.length() == 1 ){
				// prefix with 0
				b.append('0');
			}
			b.append(hex);
		}
		return b.toString();
	}

}
