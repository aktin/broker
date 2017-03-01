package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.server.DateDataSource;
import org.aktin.broker.xml.ResultInfo;

public interface AggregatorBackend {

	void setBrokerDB(DataSource ds);

	/**
	 * Delete all files in the data directory.
	 * This should be used only for testing.
	 * @throws IOException IO error
	 */
	void clearDataDirectory() throws IOException;

	List<ResultInfo> listResults(int requestId) throws SQLException;

	DateDataSource getResult(int requestId, int nodeId) throws SQLException;

	void addOrReplaceResult(int requestId, int nodeId, MediaType mediaType, InputStream content) throws SQLException;

}