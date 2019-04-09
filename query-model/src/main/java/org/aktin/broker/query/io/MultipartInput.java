package org.aktin.broker.query.io;

public interface MultipartInput {
	/**
	 * Get parts
	 * @return multipart entry
	 */
	Iterable<MultipartEntry> getEntries();

}
