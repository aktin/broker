package org.aktin.broker.query.sql;

import java.io.IOException;

public interface TableWriter extends AutoCloseable{
	void header(String ... headers)throws IOException;
	void row(String ... data) throws IOException;
	@Override
	void close() throws IOException;
}