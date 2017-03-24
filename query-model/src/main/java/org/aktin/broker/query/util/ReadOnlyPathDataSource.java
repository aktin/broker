package org.aktin.broker.query.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.activation.DataSource;

public class ReadOnlyPathDataSource implements DataSource {
	private String type;
	private Path path;

	public ReadOnlyPathDataSource(Path path, String type){
		this.path = path;
		this.type = type;
	}
	@Override
	public String getContentType() {
		return type;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return Files.newInputStream(path);
	}

	@Override
	public String getName() {
		return path.getFileName().toString();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException();
	}

}
