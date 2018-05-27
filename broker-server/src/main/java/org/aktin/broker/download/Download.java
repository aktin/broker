package org.aktin.broker.download;

import java.util.UUID;

import org.aktin.broker.server.DateDataSource;

public interface Download extends DateDataSource{
	
	public UUID getId();
	public long getExpireTimestamp();
}
