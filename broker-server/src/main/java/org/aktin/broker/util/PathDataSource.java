package org.aktin.broker.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aktin.broker.server.DateDataSource;


public class PathDataSource implements DateDataSource {
	private static final Logger log = Logger.getLogger(PathDataSource.class.getName());

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
	public String toString() {
		return "PathDataSource(path="+path.toString()+", type="+type+")";
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
	@Override
	public Long getContentLength() {
		try {
			return Files.size(path);
		} catch (IOException e) {
			log.log(Level.WARNING, "Unable to determine file size", e);
			return null;
		}
	}

	public Path getPath() {
		return path;
	}
}
