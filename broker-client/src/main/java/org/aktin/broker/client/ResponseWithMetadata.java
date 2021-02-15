package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;

public interface ResponseWithMetadata {

	long getLastModified();

	byte[] getMD5();

	String getMD5String();

	String getContentType();

	String getName();

	InputStream getInputStream() throws IOException;

}