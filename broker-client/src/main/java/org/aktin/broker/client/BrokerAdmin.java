package org.aktin.broker.client;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.RequestStatusList;
import org.aktin.broker.xml.ResultInfo;
import org.w3c.dom.Node;

public interface BrokerAdmin {

	void setEndpoint(URI brokerEndpoint);
	
	/**
	 * Get the the request definition with the specified media type.
	 * If the request id does not exist or the mediaType is not available, {@code null} is returned.
	 *
	 * @param requestId request id
	 * @param mediaType desired media type.
	 * @return
	 * @throws IOException
	 */
	Reader getRequestDefinition(int requestId, String mediaType) throws IOException;

	// TODO return actual media type
	Reader getRequestNodeMessage(int requestId, int nodeId) throws IOException;

	// TODO also return media type, e.g. via Datasource wrapping HttpURLConnection
	ResponseWithMetadata getNodeResource(int nodeId, String resourceId) throws IOException;

	<T> T getNodeResourceJAXB(int nodeId, String resourceId, Class<T> type) throws IOException;
	
	/**
	 * @param nodeId     the ID of the node to retrieve the resource for
	 * @param resourceId the name of the resource to retrieve
	 * @return a map containing key-value pairs representing the properties of the requested resource for the specified
	 *         node or {@code null} if the resource is non-existing (or something went wrong).
	 * @throws IOException
	 */
	Properties getNodeProperties(int nodeId, String resourceId) throws IOException;

	String getNodeString(int nodeId, String resourceId) throws IOException;
	
	/**
	 * Allocate a request without content
	 * <p>
	 * The ID of the allocated request is not in the body of the response, but the complete URI is written
	 * to "Location" inside the header
	 *
	 * @return The ID of the newly allocated request
	 * @throws IOException IO error
	 */
	int createRequest() throws IOException;

	/**
	 * Create a request with specified content type and content
	 * @param contentType content type
	 * @param writer writer for the content
	 * @return request id
	 * @throws IOException IO error
	 */
//	@Deprecated
//	int createRequest(String contentType, OutputWriter writer) throws IOException;
//
//	@Deprecated
//	int createRequest(String contentType, InputStream content) throws IOException;

	int createRequest(String contentType, Node content) throws IOException;

	int createRequest(String contentType, String content) throws IOException;
	
	/**
	 * If the targeted request does not exist on the Broker, still the code 204 is returned.
	 * If a published request is deleted, the client will also delete it locally after being notified.
	 *
	 * @param requestId The ID of the request to delete.
	 * @throws IOException IO error
	 */
	void deleteRequest(int requestId) throws IOException;
	
	/**
	 * A request can also be published without a set definition or without set targets.
	 * A published request can be published again. In this case the content is updated on the client's side after retrieval.
	 *
	 * @param requestId The ID of the request to publish.
	 */
	void publishRequest(int requestId) throws IOException;
	
	/**
	 * Once a request is closed, it cant be open again and all interaction with the nodes stops
	 *
	 * @param requestId The ID of the request to close.
	 */
	void closeRequest(int requestId) throws IOException;

//	@Deprecated
//	void putRequestDefinition(int requestId, String contentType, OutputWriter writer) throws IOException;
	
	/**
	 * Add/Update content of an existing request
	 * If the targeted request does not exist on the Broker, still the code 204 is returned.
	 * The definition of a published request can be updated, but SHOULD BE AVOIDED at all costs, to avoid inconsistencies.
	 *
	 * @param requestId The ID of the request to add the definition to.
	 */
	void putRequestDefinition(int requestId, String contentType, String content) throws IOException;
	
	/**
	 * @return a list of {@link org.aktin.broker.xml.Node}
	 * @throws IOException
	 */
	List<org.aktin.broker.xml.Node> listNodes() throws IOException;
	
	/**
	 * @param nodeId the ID of the node to retrieve.
	 * @return the {@link Node} with the specified ID, or null if no such node exists (or something went wrong).
	 * @throws IOException
	 */
	org.aktin.broker.xml.Node getNode(int nodeId) throws IOException;
	
	/**
	 * @return A list of available {@link RequestInfo}
	 * @throws IOException
	 */
	List<RequestInfo> listAllRequests() throws IOException;
	
	/**
	 * Retrieve request info. This info does not include the node status.
	 *
	 * @param requestId The ID of the request to retrieve information for.
	 * @return A {@link RequestInfo} object containing timestamps and meta information about the request.
	 * @throws IOException
	 */
	RequestInfo getRequestInfo(int requestId) throws IOException;
	
	/**
	 * If request does not exist, an empty {@link RequestStatusList} is returned.
	 *
	 * @param requestId The ID of the request to retrieve status for.
	 * @return A {@link RequestStatusList} object containing a list of {@link RequestStatusInfo} with timestamp information of targeted broker nodes.
	 * @throws IOException
	 */
	List<RequestStatusInfo> listRequestStatus(int requestId) throws IOException;

	List<ResultInfo> listResults(int requestId) throws IOException;

	// TODO ResultInfo getResultInfo(int requestId, String nodeId)
	
	String getResultString(int requestId, int nodeId) throws IOException;
	ResponseWithMetadata getResult(int requestId, int nodeId) throws IOException;
	
	String getAggregatedResultUUID(int requestId) throws IOException;
	ResponseWithMetadata getAggregatedResult(String uuid) throws IOException;
	
	/**
	 * Retrieves an array of target nodes that a request is aimed at based on the specified request ID.
	 *
	 * <p>If no target restrictions are found for the request (the broker returns null), this method also
	 * returns null. Otherwise, it returns the array of target node IDs associated with the request.</p>
	 *
	 * @param requestId the ID of the request for which target nodes are to be retrieved
	 * @return an array of integer IDs representing the target nodes for the request,
	 *         or null if there is no target restriction
	 * @throws IOException if there's a problem with the communication with the broker
	 */
	int[] getRequestTargetNodes(int requestId) throws IOException;
	
	/**
	 * If the targeted request does not exist on the Broker, still the code 204 is returned.
	 * If no target nodes are set, the published request will be sent to all connected nodes (even retroactively to nodes that joined after the publish date).
	 * Targeted nodes are not required to exist on the broker.
	 *
	 * @param requestId the ID of the request for which to set the target nodes.
	 * @param nodes     a list of ids corresponding to the targeted nodes.
	 * @throws IOException
	 */
	void setRequestTargetNodes(int requestId, int[] nodes) throws IOException;
	
	/**
	 * @param requestId the ID of the request for which to delete the target nodes
	 * @throws IOException
	 */
	
	/**
	 * Clears the target nodes of a given request based on the specified request ID.
	 *
	 * @param requestId the ID of the request for which target nodes are to be cleared
	 * @throws IOException if there's a problem with the communication with the broker
	 */
	void clearRequestTargetNodes(int requestId) throws IOException;
}