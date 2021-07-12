package org.aktin.broker.server;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.RequestStatusInfo;

public interface Broker {
	List<Node> getAllNodes() throws SQLException;
	Node getNode(int nodeId) throws SQLException;

	int createRequest(String mediaType, Reader content) throws SQLException;
	/**
	 * Create request without specifying content yet.
	 * The request definition must be set afterwards via {@link #setRequestDefinition(int, String, Reader)}
	 * @return request id
	 * @throws SQLException database error
	 */
	int createRequest() throws SQLException;

	void setRequestDefinition(int requestId, String mediaType, Reader content) throws SQLException;

	void deleteRequest(int id) throws SQLException;

	List<RequestInfo> listAllRequests() throws SQLException;

	List<RequestInfo> searchAllRequests(String mediaType, String searchLanguage, String predicate) throws IOException;
	
	List<String> getRequestTypes(int requestId) throws SQLException;

	/**
	 * Get the request definition source for the specified request and media type.
	 * <p>
	 * since the connection will be closed in this method, we cannot stream the CLOB 
	 * directly from the database.
	 * Therefore, we will read the CLOB into memory if small, or a temporary file, if large.
	 * </p>
	 * 
	 * @param requestId request id
	 * @param mediaType media type
	 * @return reader providing the definition source
	 * @throws SQLException sql error
	 * @throws IOException io error during CLOB buffering
	 */
	Reader getRequestDefinition(int requestId, String mediaType) throws SQLException, IOException;

	// TODO allow flags to retrieve unpublished and/or closed requests
	List<RequestInfo> listRequestsForNode(int nodeId) throws SQLException;

	void setRequestNodeStatus(int requestId, int nodeId, RequestStatus status, Instant timestamp) throws SQLException;

	/**
	 * Set the node status message. The status must be set previously e.g. using {@link #setRequestNodeStatus(int, int, RequestStatus, Instant)},
	 * If there is no status entry for the specified node, this method will fail.
	 * 
	 * @param requestId request id
	 * @param nodeId node id
	 * @param messageType media type for the status message
	 * @param message content of the status message
	 * @throws SQLException sql errror
	 */
	void setRequestNodeStatusMessage(int requestId, int nodeId, String messageType, Reader message) throws SQLException;

	/**
	 * Mark a request as deleted for a given node
	 * 
	 * @param nodeId node id
	 * @param requestId request id
	 * @return {@code true} if the request could be marked as deleted. {@code false} if the request was already deleted or could not be found
	 * @throws SQLException sql error
	 */
	boolean markRequestDeletedForNode(int nodeId, int requestId) throws SQLException;

	/**
	 * Retrieve list with node status for the specified request.
	 * @param requestId request id
	 * @return Status list or {@code null} if request is not found.
	 * @throws SQLException SQL error
	 */
	List<RequestStatusInfo> listRequestNodeStatus(Integer requestId) throws SQLException;

	/**
	 * Get basic status info for the given request id.
	 * @param requestId request id
	 * @return request info or {@code null} if not found
	 * @throws SQLException SQL error
	 */
	RequestInfo getRequestInfo(int requestId) throws SQLException;
	
	void setRequestPublished(int requestId, Instant timestamp) throws SQLException;
	void setRequestClosed(int requestId, Instant timestamp) throws SQLException;
	void setRequestTargets(int requestId, int[] nodes) throws SQLException;
	int[] getRequestTargets(int requestId) throws SQLException;
	void clearRequestTargets(int requestId) throws SQLException;


	/**
	 * Retrieve the status message reported by the specified node for the specified request.
	 * TODO also return media type.
	 * @param requestId request id
	 * @param nodeId node id
	 * @return reader for the status message
	 * @throws SQLException SQL error
	 * @throws IOException IO error
	 */
	Reader getRequestNodeStatusMessage(int requestId, int nodeId) throws SQLException, IOException;
	DateDataSource getNodeResource(int nodeId, String resourceId) throws SQLException;
}
