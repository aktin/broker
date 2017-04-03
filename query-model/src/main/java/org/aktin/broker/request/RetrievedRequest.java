package org.aktin.broker.request;

import java.io.IOException;

import javax.activation.DataSource;

import org.aktin.broker.query.xml.QueryRequest;

public interface RetrievedRequest {

	public int getRequestId();
	public QueryRequest getRequest();
	public RequestStatus getStatus();
	public Iterable<ActionLogEntry> getActionLog();
	public boolean hasAutoSubmit();
	/**
	 * Get the result data. Result data is only available,
	 * after a call to {@link #createResultData(String)}.
	 * <p>
	 * Use this method is also used to write the result data
	 * via {@link DataSource#getOutputStream()}.
	 * </p>
	 * @return result data
	 */
	public DataSource getResultData() throws IOException;
	void addToActionLog(String userId, RequestStatus from, RequestStatus to, String description) throws IOException;
	/**
	 * Create the result data. To write the result
	 * data, use {@link DataSource#getOutputStream()} on
	 * the result of {@link #getResultData()}.
	 * @param mediaType media type to use
	 */
	void createResultData(String mediaType) throws IOException;
	/**
	 * Removes the result data. Use this method e.g. if writing
	 * to the result failed. After a call to this method,
	 * {@link #getResultData()} will return {@code null} again.
	 *
	 * @throws IOException IO error
	 */
	void removeResultData() throws IOException;
}
