package org.aktin.broker.query;

import java.io.IOException;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * Handler for a single query. Allows
 * visualization, execution and result
 * conversion.
 * @author R.W.Majeed
 *
 */
public interface QueryHandler {

	DataSource getQueryVisualisation(String mediaType) throws IOException;

	
	/**
	 * Get the native media type in which
	 * the query results are stored.
	 * @return media type
	 */
	String getResultMediaType();

	/**
	 * Execute the query and store the results
	 * in the target location.
	 * @param target output stream where the results will be stored
	 * @throws IOException execution/export failure
	 */
	void execute(MultipartOutputStream target) throws IOException;

	/**
	 * Get additional media types to which
	 * the results can be converted for visualization.
	 * <p>
	 * Typical display types are {@code text/html}, {@code text/plain}
	 * or MS Excel
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
	 * @param targetMediaType target media type
	 * @return input stream with data of target media type
	 * @throws IOException IO error
	 */
	DataSource getResultVisualisation(DataSource result, String targetMediaType) throws IOException;
}
