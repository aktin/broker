package org.aktin.broker.db;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import javax.sql.DataSource;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatus;
import org.aktin.broker.xml.RequestStatusInfo;

public interface BrokerBackend {

	void setBrokerDB(DataSource brokerDB);

	List<Node> getAllNodes() throws SQLException;
	Node getNode(int nodeId) throws SQLException;

	int createRequest(String mediaType, Reader content) throws SQLException;

	void setRequestDefinition(int requestId, String mediaType, Reader content) throws SQLException;

	void deleteRequest(int id) throws SQLException;

	List<RequestInfo> listAllRequests() throws SQLException;

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
	 * Add or retrieve the node principal. The node is identified by its key.
	 * For API-Key authentication, this can be the API key. For TLS client certificate
	 * authentication, this may be the certificate serial number.
	 * 
	 * @param nodeKey unique key for the node. E.g. Certificate serial number or API key
	 * @param clientDn optional X500 client DN string
	 * @return principal
	 * @throws SQLException SQL error
	 */
	Principal accessPrincipal(String nodeKey, String clientDn) throws SQLException;
}