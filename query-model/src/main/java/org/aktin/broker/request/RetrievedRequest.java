package org.aktin.broker.request;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import javax.activation.DataSource;

import org.aktin.broker.query.xml.QueryRequest;

public interface RetrievedRequest {

	public int getRequestId();
	public QueryRequest getRequest();
	public RequestStatus getStatus();
	public Iterable<ActionLogEntry> getActionLog() throws IOException;
	public boolean hasAutoSubmit();
	public void setAutoSubmit(boolean autoSubmit) throws IOException;

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
	 * @param properties processing properties. The properties can vary between processing steps, depending on the step.
	 * @param stepName name of the processed part. Optional and can be {@code null} if execution contains only one part.
	 * @param stepNo step number for multiple processing steps. First step will be numbered 1.
	 * @param numSteps total number of steps. This method should be called for each step
	 * 
	 * @throws IOException IO error
	 */
	void setProcessing(Map<String,String> properties, String stepName, int stepNo, int numSteps)throws IOException;

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
	 * Create directory for storing intermediate files. It is the callers responsibility
	 * to clean and delete the directory after use.
	 * @param stepNo number differentiating between multiple intermediate stages. Fist step should use {@code 1}.
	 * @return Path for storing intermediate files
	 * @throws IOException IO error
	 */
	Path createIntermediateDirectory(int stepNo) throws IOException;
	/**
	 * Removes the result data. Use this method e.g. if writing
	 * to the result failed. After a call to this method,
	 * {@link #getResultData()} will return {@code null} again.
	 *
	 * @throws IOException IO error
	 */
	void removeResultData() throws IOException;
}
