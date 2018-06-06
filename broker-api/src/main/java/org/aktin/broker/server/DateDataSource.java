package org.aktin.broker.server;

import java.time.Instant;

import javax.activation.DataSource;

public interface DateDataSource extends DataSource{

	Instant getLastModified();
	Long getContentLength();
}
