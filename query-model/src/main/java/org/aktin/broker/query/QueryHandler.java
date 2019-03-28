package org.aktin.broker.query;

import java.io.IOException;

import javax.activation.DataSource;

import org.aktin.broker.query.io.MultipartDirectory;
import org.aktin.broker.query.io.MultipartOutputStream;

/**
 * Handler for a single query. Allows
 * visualization, execution and result
 * conversion.
 * @author R.W.Majeed
 *
 */
public interface QueryHandler {
	/**
	 * Execute the query and store the results
	 * in the target location.
	 *
	 * @param input input for the execution, typically output from the 
	 *   previous processing stage. Will be {@code null} for extraction handlers.
	 *
	 * @param target output stream where the results will be stored
	 * @throws IOException execution/export failure
	 */
	void execute(MultipartDirectory input, MultipartOutputStream target) throws IOException;
}
