package org.aktin.broker.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadImpl implements Download{
	private static final Logger log = Logger.getLogger(DownloadImpl.class.getName());
	boolean deletePath;
	private Path path;
	/** unique download id */
	UUID id;
	/** epoch millis when the download expires */
	long expiration;
	private String mediaType;

	public DownloadImpl(String mediaType, long expirationTimestamp) {
		this.mediaType = mediaType;
		this.expiration = expirationTimestamp;
	}
	
	public String getMediaType() {
		return mediaType;
	}

	public long getContentLength() throws IOException {
		return Files.size(path);
	}
	public void setPath(Path path, boolean deleteOnExpiration) {
		this.path = path;
		this.deletePath = deleteOnExpiration;
	}

	@Override
	public Instant getLastModified() {
		return null;
	}

	@Override
	public String getContentType() {
		return mediaType;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return Files.newInputStream(path);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return Files.newOutputStream(path);
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public long getExpireTimestamp() {
		return expiration;
	}

	@Override
	public void postRemovalCleanup() {
		if( deletePath ) {
			try {
				Files.delete(path);
				log.info("Download "+id+" file "+path+" deleted");
			} catch (IOException e) {
				log.log(Level.WARNING, "Doanload "+id+" failed to delete file "+path, e);
			}
		}else {
			log.info("Download "+id+" removed. File remaining: "+path);
		}
	}

}
