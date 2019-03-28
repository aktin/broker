package org.aktin.broker.query;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Interface for writing output with multiple parts,
 * e.g. different tabular files and/or image files.
 *
 *
 * @author R.W.Majeed
 *
 */
public interface MultipartOutputStream {

	/**
	 * Open a new part for writing.
	 *
	 * Implementations are not required to support concurrent
	 * writing of multiple parts. Therefore, it is recommended
	 * to finish the previous part first by closing the returned
	 * OutputStream before opening the next part via {@link #writePart(String, String)}.
	 * 
	 * @param mediaType internet media type / MIME type. Required.
	 * @param filename file name. Can be {@code null}
	 * @return output stream for writing
	 * @throws IOException IO error
	 */
	OutputStream writePart(String mediaType, String filename) throws IOException;
	
}
