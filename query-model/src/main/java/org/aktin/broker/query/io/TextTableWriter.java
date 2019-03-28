package org.aktin.broker.query.io;

import java.io.BufferedWriter;
import java.io.IOException;

// TODO move to request manager
/**
 * Provide methods for writing text tables
 * with delimitors. E.g. comma separated values (CSV)
 * 
 * @author R.W.Majeed
 *
 */
public class TextTableWriter implements AutoCloseable{

	private BufferedWriter writer;
	/**
	 * Create a ZIP file export.
	 * A call to {@link #close()} will also close the output stream.
	 * @param out output stream to write the ZIP file to
	 * @param charset charset used for CSV data, zip entry names, zip comments
	 */
	public TextTableWriter(BufferedWriter writer) {
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
	
}
