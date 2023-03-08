package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.aktin.broker.client.BrokerClientImpl.OutputWriter;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatusInfo;
import org.aktin.broker.xml.ResultInfo;
import org.w3c.dom.Node;

public interface BrokerAdmin {

	void setEndpoint(URI brokerEndpoint);

	
	/**
	 * Get the the request definition with the specified media type.
	 * If the request id does not exist or the mediaType is not available, {@code null} is returned.
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

	Properties getNodeProperties(int nodeId, String resourceId) throws IOException;

	String getNodeString(int nodeId, String resourceId) throws IOException;

	/**
	 * Create a request without content. Content must be specified later
	 * via XXX
	 * @return request id
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
	@Deprecated
	int createRequest(String contentType, OutputWriter writer) throws IOException;

	@Deprecated
	int createRequest(String contentType, InputStream content) throws IOException;

	int createRequest(String contentType, Node content) throws IOException;

	int createRequest(String contentType, String content) throws IOException;

	/**
	 * Delete a single request from database and remove all associated result data.
	 * @param requestId request id to delete
	 * @throws IOException IO error
	 */
	void deleteRequest(int requestId) throws IOException;

	void publishRequest(int requestId) throws IOException;

	void closeRequest(int requestId) throws IOException;

	@Deprecated
	void putRequestDefinition(int requestId, String contentType, OutputWriter writer) throws IOException;

	void putRequestDefinition(int requestId, String contentType, String content) throws IOException;

	List<org.aktin.broker.xml.Node> listNodes() throws IOException;

	org.aktin.broker.xml.Node getNode(int nodeId) throws IOException;

	List<RequestInfo> listAllRequests() throws IOException;

	/**
	 * Retrieve request info. This info does not include the node status.
	 * 
	 * @param requestId request id
	 * @return request info
	 * @throws IOException communications error
	 */
	RequestInfo getRequestInfo(int requestId) throws IOException;

	List<RequestStatusInfo> listRequestStatus(int requestId) throws IOException;

	List<ResultInfo> listResults(int requestId) throws IOException;

	// TODO ResultInfo getResultInfo(int requestId, String nodeId)


	String getResultString(int requestId, int nodeId) throws IOException;
	ResponseWithMetadata getResult(int requestId, int nodeId) throws IOException;

	int[] getRequestTargetNodes(int requestId) throws IOException;

	void setRequestTargetNodes(int requestId, int[] nodes) throws IOException;

	void clearRequestTargetNodes(int requestId) throws IOException;
}