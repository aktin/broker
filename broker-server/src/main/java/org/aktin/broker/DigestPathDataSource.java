package org.aktin.broker;

import java.nio.file.Path;
import java.time.Instant;

public class DigestPathDataSource extends PathDataSource {

	public byte[] md5;
	public byte[] sha256;

	public DigestPathDataSource(Path path, String type, Instant lastModified) {
		super(path, type, lastModified);
	}


}
