package org.aktin.broker.query.io;

import java.nio.file.Path;

public interface MultipartDirectory extends MultipartInput {

	/**
	 * Return the base path where the multipart
	 * entries are stored.
	 * @return Path to directory containing 
	 */
	Path getBasePath();
}
