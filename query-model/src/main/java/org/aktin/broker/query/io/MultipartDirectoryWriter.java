package org.aktin.broker.query.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Multipart output writing to a Zip file
 * 
 * @author R.W.Majeed
 *
 */
public class MultipartDirectoryWriter implements MultipartOutputStream, MultipartDirectory {

	private Path dir;
	private List<MultipartEntry> entries;

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

	private class MultipartEntryImpl implements MultipartEntry{
		private String name;
		private String mediaType;

		public MultipartEntryImpl(String name, String mediaType) {
			this.name = name;
			this.mediaType = mediaType;
		}
		@Override
		public String getName() {return name;}

		@Override
		public String getMediaType() {return mediaType;}

		@Override
		public InputStream newInputStream(OpenOption... openOptions) throws IOException {
			return Files.newInputStream(dir.resolve(name), openOptions);
		}
	}
	@Override
	public OutputStream writePart(String mediaType, String name) throws IOException {
		Path file = resolveFile(mediaType, name);		
		OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW);
		entries.add(new MultipartEntryImpl(name, mediaType));
		return out;
	}

	@Override
	public BufferedWriter writeTextPart(String mediaType, String name, Charset charset) throws IOException {
		Path file = resolveFile(mediaType, name);
		BufferedWriter writer = Files.newBufferedWriter(file, charset, StandardOpenOption.CREATE_NEW);
		entries.add(new MultipartEntryImpl(name, mediaType+";charset="+charset.name()));
		return writer;
	}

	@Override
	public Path getBasePath() {
		return dir;
	}

	@Override
	public Iterable<MultipartEntry> getEntries() {
		return entries;
	}
}
