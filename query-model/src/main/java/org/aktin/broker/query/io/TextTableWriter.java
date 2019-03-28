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

	public static final String MEDIA_TYPE_CSV = "text/comma-separated-values";
	public static final String MEDIA_TYPE_TAB = "text/tab-separated-values";

	private String fieldSeparator;
	private String recordSeparator;
	/** String to write to the file instead of a {@code null} value. Defaults to empty string. */
	private String nullString;
	private BufferedWriter writer;
	/**
	 * Create a ZIP file export.
	 * A call to {@link #close()} will also close the output stream.
	 * @param out output stream to write the ZIP file to
	 * @param charset charset used for CSV data, zip entry names, zip comments
	 */
	public TextTableWriter(BufferedWriter writer) {
		fieldSeparator = "\t";
		recordSeparator = "\r\n";//System.lineSeparator();
		nullString = "";
	}

	public void writeRecord(String[] values) throws IOException{
		for( int i=0; i<values.length; i++ ){
			if( i != 0 ){
				writer.write(fieldSeparator);
			}
			if( values[i] != null ){
				writer.write(values[i]);
			}else{
				writer.write(nullString);
			}
		}
		writer.write(recordSeparator);
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
	
}
