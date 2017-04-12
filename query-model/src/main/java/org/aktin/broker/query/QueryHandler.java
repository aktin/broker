package org.aktin.broker.query;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

import javax.activation.DataSource;

/**
 * Handler for a single query. Allows
 * visualization, execution and result
 * conversion.
 * @author R.W.Majeed
 *
 */
public interface QueryHandler {

	Reader getQueryHTML();

	
	/**
	 * Get the native media type in which
	 * the query results are stored.
	 * @return media type
	 */
	String getResultMediaType();

	/**
	 * Execute the query and store the results
	 * in the target location.
	 * @param target
	 * @return exported data
	 * @throws IOException execution/export failure
	 */
	void execute(OutputStream target) throws IOException;

	/**
	 * Get additional media types to which
	 * the results can be converted for visualization.
	 * <p>
	 * Typical display types are {@code text/html}
	 * and MS Excel
	 * </p>
	 * @return array with media types to display results
	 */
	String[] getResultDisplayTypes();

	/**
	 * Convert result data for display.
	 * Target media type must be one of the values
	 * returned by {@link #getResultDisplayTypes()}.
	 *
	 * @param result result data
	 * @param mediaType target media type
	 * @return input stream with data of target media type
	 */
	InputStream getResultDisplayData(DataSource result, String mediaType);
}
