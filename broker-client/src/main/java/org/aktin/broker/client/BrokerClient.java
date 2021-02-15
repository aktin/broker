package org.aktin.broker.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.aktin.broker.client.BrokerClientImpl.OutputWriter;
import org.aktin.broker.xml.BrokerStatus;
import org.aktin.broker.xml.Node;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatus;
import org.w3c.dom.Document;

public interface BrokerClient {

	public void setAggregatorEndpoint(URI uri);
	/**
	 * Set the endpoint URI. This method will be called automatically 
	 * from the constructor. If using a different constructor,
	 * make sure that the endpoint URI is set before any client method is
	 * called.
	 * Setting the endpoint will automatically call {@link #setAggregatorEndpoint(URI)}
	 * to set the aggregator endpoint accordingly. If a different aggregator endpoint should
	 * be used, call {@link #setAggregatorEndpoint(URI)} manually after this method.
	 * 
	 * @param endpointURI endpoint URI
	 */
	public void setEndpoint(URI endpointURI);

	/**
	 * Retrieve status information about the broker.
	 * @return broker status information
	 * @throws IOException io error
	 */
	public BrokerStatus getBrokerStatus() throws IOException;

	/**
	 * Post the supplied software versions under a resource {@code versions}.
	 * Due to limitations in the way Java persists {@link Properties}, {@code null} values
	 * are not allowed.
	 * @param softwareVersions map with software names as keys and versions as values.
	 * @throws IOException IO error during transmission
	 * @throws NullPointerException for {@code null} values in the map
	 */
	void postSoftwareVersions(Map<String, String> softwareVersions) throws IOException, NullPointerException;

	/**
		 * Post status for the client node. Additional software modules can be specified.
		 * 
		 * @param startupEpochMillis startup time in epoch milliseconds
		 * @param softwareVersions software module versions
		 * @param payload Status content / payload. Can be any JAXB compatible object ({@code @XMLRootElement} or {@link Element})
		 * @throws IOException IO error
		 */
	//	@Deprecated
	//	public void postMyStatus(long startupEpochMillis, Map<String,String> softwareVersions) throws IOException{
	//		postSoftwareVersions(softwareVersions);
	//	}
	/**
	 * Get list of requests
	 * @return request list
	 * @throws IOException error
	 */
	List<RequestInfo> listMyRequests() throws IOException;

	/**
	 * Get info for a single request
	 * @param id request id
	 * @return request list
	 * @throws IOException error
	 */
	RequestInfo getRequestInfo(int id) throws IOException;

	void deleteMyRequest(int id) throws IOException;

	Reader getMyRequestDefinitionReader(int id, String mediaType) throws IOException;

	Node getMyNode() throws IOException;

	Document getMyRequestDefinitionXml(int id, String mediaType) throws IOException;

	@Deprecated
	String[] getMyRequestDefinitionLines(int id, String mediaType) throws IOException;

	String getMyRequestDefinitionString(int id, String mediaType) throws IOException;

	//	@SuppressWarnings("unchecked")
	//	public <T> T getMyRequestDefinition(String id, String mediaType, Class<T> type) throws IOException{
	//		if( type == String.class ){
	//			return (T)getMyRequestDefinitionString(id, mediaType);
	//		}else{
	//			throw new IllegalArgumentException("Unsupported type "+type);
	//		}
	//	}
	void putMyResource(String name, String contentType, InputStream content) throws IOException;

	void putMyResourceXml(String name, Object jaxbObject) throws IOException;

	void putMyResourceProperties(String name, Properties properties) throws IOException;

	Properties getMyResourceProperties(String name) throws IOException;

	ResponseWithMetadata getMyResource(String name) throws IOException;

	void putMyResource(String name, String contentType, String content) throws IOException;

	void deleteMyResource(String name) throws IOException;

	@Deprecated
	void putRequestResult(int requestId, String contentType, OutputWriter writer) throws IOException;

	void putRequestResult(int requestId, String contentType, InputStream content) throws IOException;

	void putRequestResult(int requestId, String contentType, String content) throws IOException;

	void postRequestStatus(int requestId, RequestStatus status) throws IOException;

	/**
	 * Report that a request has been failed.
	 * @param requestId failed request's id
	 * @param message error message. Can be {@code null}.
	 * @param throwable throwable. Can be {@code null}.
	 * @throws IOException IO error
	 */
	void postRequestFailed(int requestId, String message, Throwable throwable) throws IOException;

	void postRequestStatus(int requestId, RequestStatus status, Instant date, String description) throws IOException;

}