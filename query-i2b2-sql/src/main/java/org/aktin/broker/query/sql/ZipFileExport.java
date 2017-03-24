package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// TODO move to request manager
/**
 * Creates a Zip-File containing tab separated
 * table files.
 * 
 * @author R.W.Majeed
 *
 */
public class ZipFileExport implements TableExport {

	private ZipOutputStream zip;
	private String fieldSeparator;
	private String recordSeparator;
	private CharsetEncoder encoder;
	private CharBuffer buffer;
	/**
	 * Create a ZIP file export.
	 * @param out output stream
	 * @param charset charset used for CSV data, zip entry names, zip comments
	 */
	public ZipFileExport(OutputStream out, Charset charset) {
		zip = new ZipOutputStream(out, charset);
		fieldSeparator = "\t";
		recordSeparator = "\n";
		encoder = charset.newEncoder();
		buffer = CharBuffer.allocate(1024*16);
	}

	private void writeRecord(String[] values) throws IOException{
		buffer.clear();
		for( int i=0; i<values.length; i++ ){
			if( i != 0 ){
				buffer.put(fieldSeparator);
			}
			buffer.put(values[i]);
		}
		buffer.put(recordSeparator);
		ByteBuffer bytes = encoder.encode(buffer);
		zip.write(bytes.array(), bytes.arrayOffset(), bytes.arrayOffset()+bytes.limit());
	}
	@Override
	public TableWriter exportTable(String name) throws IOException {
		ZipEntry ze = new ZipEntry(name);
		zip.putNextEntry(ze);
		return new TableWriter() {
			
			@Override
			public void row(String[] columns) throws IOException {
				writeRecord(columns);
			}
			
			@Override
			public void header(String[] headers) throws IOException {
				writeRecord(headers);
			}
			
			@Override
			public void close() throws IOException {
				zip.closeEntry();
			}
		};
	}

	@Override
	public void close() throws IOException {
		zip.close();
	}

}
