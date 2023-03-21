package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import javax.sql.DataSource;
import jakarta.ws.rs.core.MediaType;

import org.aktin.broker.server.Aggregator;

public interface AggregatorBackend extends Aggregator{

	void setBrokerDB(DataSource ds);

	/**
	 * Delete all files in the data directory.
	 * This should be used only for testing.
	 * @throws IOException IO error
	 */
	void clearDataDirectory() throws IOException;

	void addOrReplaceResult(int requestId, int nodeId, MediaType mediaType, InputStream content) throws SQLException;

	boolean isRequestWritable(int requestId, int nodeId);
}