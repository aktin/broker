package org.aktin.broker;

import java.time.Instant;

import javax.activation.DataSource;

public interface DateDataSource extends DataSource{

	Instant getLastModified();
}
