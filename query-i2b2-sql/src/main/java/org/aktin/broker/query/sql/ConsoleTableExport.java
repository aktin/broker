package org.aktin.broker.query.sql;

import java.io.IOException;

import org.aktin.broker.query.io.table.TableExport;
import org.aktin.broker.query.io.table.TableWriter;

public class ConsoleTableExport implements TableExport, TableWriter {

	@Override
	public void header(String... headers) throws IOException {
		System.out.println(String.join("\t", headers));
	}

	@Override
	public void row(String... columns) throws IOException {
		System.out.println(String.join("\t", columns));
	}

	@Override
	public void close() throws IOException {
		System.out.println("--");
	}

	@Override
	public TableWriter exportTable(String name) throws IOException {
		System.out.println("TABLE("+name+")");
		System.out.println("-");
		return this;
	}

}
