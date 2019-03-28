package org.aktin.broker.query.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Multipart output writing to a Zip file
 * 
 * @author R.W.Majeed
 *
 */
public class ZipArchiveWriter implements MultipartOutputStream {

	private ZipOutputStream zip;
	/**
	 * Create a ZIP file export.
	 * A call to {@link #close()} will also close the output stream.
	 * @param out output stream to write the ZIP file to
	 * @param charset charset used for CSV data, zip entry names, zip comments
	 */
	public ZipArchiveWriter(OutputStream out, Charset charset) {
		zip = new ZipOutputStream(out, charset);
	}

	private class WrappedOutputStream extends FilterOutputStream{
		
		public WrappedOutputStream() {
			super(ZipArchiveWriter.this.zip);
		}

		@Override
		public void close() throws IOException {
			// don't close the Zip stream
			flush();
		}		
	}


	@Override
	public void close() throws IOException {
		zip.close();
	}

	@Override
	public OutputStream writePart(String mediaType, String name) throws IOException {
		ZipEntry ze = new ZipEntry(name);
		ze.setComment(mediaType);
		zip.putNextEntry(ze);
		return new WrappedOutputStream();
	}

}
