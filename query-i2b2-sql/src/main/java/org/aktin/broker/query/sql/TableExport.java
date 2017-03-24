package org.aktin.broker.query.sql;

import java.io.Closeable;
import java.io.IOException;

public interface TableExport extends Closeable{

	TableWriter exportTable(String name) throws IOException;
	void close() throws IOException;
}
