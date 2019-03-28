package org.aktin.broker.query.io;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * Interface for writing output with multiple parts,
 * e.g. different tabular files and/or image files.
 *
 *
 * @author R.W.Majeed
 *
 */
public interface MultipartOutputStream extends Closeable{

	/**
	 * Open a new part for writing.
	 *
	 * Implementations are not required to support concurrent
	 * writing of multiple parts. Therefore, it is recommended
	 * to finish the previous part first by closing the returned
	 * OutputStream before opening the next part via {@link #writePart(String, String)}.
	 * 
	 * @param mediaType internet media type / MIME type. Required.
	 * @param name file name. Can be {@code null}. Can contain file name extension.
	 * @return output stream for writing
	 * @throws IOException IO error
	 */
	OutputStream writePart(String mediaType, String name) throws IOException;

	default BufferedWriter writeTextPart(String mediaType, String name, Charset charset) throws IOException{
		return new BufferedWriter(new OutputStreamWriter(writePart(mediaType, name), charset));
	}
}