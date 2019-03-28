package org.aktin.broker.query.io;

import java.nio.file.Path;

public interface MultipartDirectory {

	/**
	 * Return the base path where the multipart
	 * entries are stored.
	 * @return Path to directory containing 
	 */
	Path getBasePath();
	Iterable<MultipartEntry> getEntries();
}
