package org.aktin.broker.node;

import java.io.IOException;
import java.util.List;

import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.xml.RequestInfo;

public interface RequestProcessor extends AutoCloseable{

	/**
	 * Checks whether the request can be handled by this processor.
	 * This method can be used before a call to  
	 * @param info request information
	 * @return {@code true} if the request can be processed. {@code false} otherwise.
	 */
	public boolean canHandleRequest(RequestInfo info);

	/**
	 * Process a list of requests for the given broker. If there are no requests,
	 * the method will be called with an empty list.
	 *
	 * <p>
	 * The implementing method is responsible for reporting status, deleting requests, etc.
	 * Some implementations may need to store references to asynchronous queries locally,
	 * these can also be updated during a call (e.g. when an empty list is provided because
	 * there are no new requests).
	 * 
	 * @param requests requests
	 * @param broker broker
	 * @throws IOException error
	 */
	public void processRequests(List<RequestInfo> requests, BrokerClient broker) throws IOException;
}
