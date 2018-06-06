package org.aktin.broker.download;

import java.util.UUID;

abstract class AbstractDownload implements Download {

	/** epoch millis when the download expires */
	long expiration;
	/** unique download id */
	UUID id;


	@Override
	public long getExpireTimestamp() {
		return expiration;
	}

	@Override
	public UUID getId() {
		return id;
	}


	/**
	 * Called after the download has been removed
	 * from the download manager and can not be downloaded
	 * anymore. Occurse e.g. after expiration.
	 */
	abstract void postRemovalCleanup();

}
