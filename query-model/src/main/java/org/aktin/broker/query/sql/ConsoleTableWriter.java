package org.aktin.broker.query.sql;

import java.io.IOException;

public class ConsoleTableWriter implements TableWriter {

	@Override
	public void header(String[] headers) throws IOException {
		System.out.println(String.join("\t", headers));
	}

	@Override
	public void row(String[] columns) throws IOException {
		System.out.println(String.join("\t", columns));
	}

	@Override
	public void close() throws IOException {
		System.out.println("-- end of table --");
	}

}
