package org.aktin.broker.request;

import java.io.IOException;
import java.util.Map;

import javax.activation.DataSource;

import org.aktin.broker.query.xml.QueryRequest;

public interface RetrievedRequest {

	public int getRequestId();
	public QueryRequest getRequest();
	public RequestStatus getStatus();
	public Iterable<ActionLogEntry> getActionLog();
	public boolean hasAutoSubmit();
	public void setAutoSubmit(boolean autoSubmit);

	public Marker getMarker();
	public void setMarker(Marker marker)throws IOException;
	
	// TODO methods for displaying the query and result data

	/**
	 * Get the result data. Result data is only available,
	 * after a call to {@link #createResultData(String)}.
	 * <p>
	 * Use this method is also used to write the result data
	 * via {@link DataSource#getOutputStream()}.
	 * </p>
	 * @return result data
	 * @throws IOException IO error
	 */
	public DataSource getResultData() throws IOException;
	/**
	 * Timestamp of the last action performed on this request.
	 * @return timestamp in epoch milliseconds
	 */
	public long getLastActionTimestamp();

	
	/**
	 * Set processing properties and change the status
	 * to {@link RequestStatus#Processing}.
	 *
	 * @param properties processing properties
	 * @throws IOException IO error
	 */
	void setProcessing(Map<String,String> properties)throws IOException;

	/**
	 * Change the status of the request. This will fire a status change event.
	 * @param userId user id who changed the status, or {@code null}
	 * @param newStatus new status
	 * @param description optional description
	 * @throws IOException IO error
	 */
	void changeStatus(String userId, RequestStatus newStatus, String description) throws IOException;
///	void addToActionLog(String userId, RequestStatus from, RequestStatus to, String description) throws IOException;
	/**
	 * Create the result data. To write the result
	 * data, use {@link DataSource#getOutputStream()} on
	 * the result of {@link #getResultData()}.
	 * @param mediaType media type to use
	 * @throws IOException IO error
	 * @throws NullPointerException if mediaType is null
	 */
	void createResultData(String mediaType) throws IOException, NullPointerException;
	/**
	 * Removes the result data. Use this method e.g. if writing
	 * to the result failed. After a call to this method,
	 * {@link #getResultData()} will return {@code null} again.
	 *
	 * @throws IOException IO error
	 */
	void removeResultData() throws IOException;
}
