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


}
