package org.aktin.broker.query.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Multipart output writing to a Zip file
 * 
 * @author R.W.Majeed
 *
 */
public class MultipartDirectoryWriter implements MultipartOutputStream {

	private Path dir;
	/**
	 * Create a ZIP file export.
	 * A call to {@link #close()} will also close the output stream.
	 * @param out output stream to write the ZIP file to
	 * @param charset charset used for CSV data, zip entry names, zip comments
	 */
	public MultipartDirectoryWriter(Path baseDirectory, Charset charset) {
		this.dir = baseDirectory;
	}

	@Override
	public void close() throws IOException {
		// nothing to do.
		// XXX maybe keep track of outputstreams returned by writePart and close remaining here
	}

	private Path resolveFile(String mediaType, String name) {
		// TODO maybe create subdirecories passed in name
		return dir.resolve(name);		
	}
	@Override
	public OutputStream writePart(String mediaType, String name) throws IOException {
		Path file = resolveFile(mediaType, name);
		return Files.newOutputStream(file, StandardOpenOption.CREATE_NEW);
	}

	@Override
	public BufferedWriter writeTextPart(String mediaType, String name, Charset charset) throws IOException {
		Path file = resolveFile(mediaType, name);
		return Files.newBufferedWriter(file, charset, StandardOpenOption.CREATE_NEW);
	}
}
