package org.aktin.broker.query.io;

import java.io.IOException;
import java.io.InputStream;

public interface MultipartEntry {
	String getName();
	String getMediaType();
	InputStream newInputStream() throws IOException;
}
