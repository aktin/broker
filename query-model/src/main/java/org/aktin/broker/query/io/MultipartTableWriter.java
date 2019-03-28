package org.aktin.broker.query.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Write text table data into a {@link MultipartOutputStream}.
 *
 * @author R.W.Majeed
 *
 */
public class MultipartTableWriter implements TableExport {
	public static final String MEDIA_TYPE_TAB = "text/tab-separated-values";
	public static final String MEDIA_TYPE_CSV = "text/comma-separated-values";

	private String fieldSeparator;
	private String recordSeparator;
	/** String to write to the file instead of a {@code null} value. Defaults to empty string. */
	private String nullString;
	private Charset charset;
	private MultipartOutputStream output;

	/**
	 * Create an instance by wrapping a {@link MultipartOutputStream}. Important: the underlying
	 * stream is not closed, when the table export is closed.
	 * @param output underlying stream, will not be closed by this class.
	 */
	public MultipartTableWriter(MultipartOutputStream output, Charset charset) {
		this.output = output;
		this.charset = charset;
		this.fieldSeparator = "\t";
		this.recordSeparator = "\r\n";
		this.nullString = "";
	}

	private class TableWriterImpl implements TableWriter{
		private BufferedWriter writer;

		public TableWriterImpl(BufferedWriter writer) {
			this.writer = writer;
		}
		private void writeRecord(String[] values) throws IOException{
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
		public void header(String... headers) throws IOException {
			writeRecord(headers);
		}

		@Override
		public void row(String... data) throws IOException {
			writeRecord(data);
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}
	}
	@Override
	public TableWriter exportTable(String name) throws IOException {
		// make sure filename has extension
		if( !name.endsWith(".txt") ) {
			name = name + ".txt";
		}
		return new TableWriterImpl(output.writeTextPart(MEDIA_TYPE_TAB, name, charset));
	}

	/**
	 * This close method does not close the underlying {@link MultipartOutputStream}.
	 */
	@Override
	public void close() throws IOException {
	}

}
