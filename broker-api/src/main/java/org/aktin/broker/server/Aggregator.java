package org.aktin.broker.server;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.aktin.broker.xml.ResultInfo;

public interface Aggregator {
	List<ResultInfo> listResults(int requestId) throws SQLException;

	String[] getDistinctResultTypes(int requestId) throws SQLException;

	DateDataSource getResult(int requestId, int nodeId) throws SQLException;

	void deleteResults(int requestId) throws SQLException, IOException;
}
