package org.aktin.broker.download;

import java.util.UUID;

import org.aktin.broker.server.DateDataSource;

public interface Download extends DateDataSource{
	
	public UUID getId();
	public long getExpireTimestamp();

	/**
	 * Called after the download has been removed
	 * from the download manager and can not be downloaded
	 * anymore. Occurse e.g. after expiration.
	 */
	public void postRemovalCleanup();
}
