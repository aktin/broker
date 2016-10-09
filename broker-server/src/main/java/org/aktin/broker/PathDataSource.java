package org.aktin.broker;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;


public class PathDataSource implements DateDataSource {

	private Path path;
	private String type;
	private Instant lastModified;

	public PathDataSource(Path path, String type, Instant lastModified){
		this.path = path;
		this.type = type;
		this.lastModified = lastModified;
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
		return Files.newOutputStream(path);
	}
	@Override
	public Instant getLastModified() {
		return lastModified;
	}

}