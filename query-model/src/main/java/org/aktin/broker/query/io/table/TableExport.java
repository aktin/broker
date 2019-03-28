package org.aktin.broker.query.io.table;

import java.io.Closeable;
import java.io.IOException;

public interface TableExport extends Closeable{

	TableWriter exportTable(String name) throws IOException;
}
