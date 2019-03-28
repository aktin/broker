package org.aktin.broker.query.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;

public interface MultipartEntry {
	String getName();
	String getMediaType();
	InputStream newInputStream(OpenOption... openOptions) throws IOException;
}
