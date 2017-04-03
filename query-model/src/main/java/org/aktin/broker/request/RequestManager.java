package org.aktin.broker.request;

public interface RequestManager {

	Iterable<RetrievedRequest> getRequests(boolean hidden);
	RetrievedRequest getRequest(int id);
	/**
	 * Get requests associated with the specified query.
	 * All returned requests belong to the same query id.
	 * @param queryId query id
	 * @return requests belonging to the specified query id
	 */
	Iterable<RetrievedRequest> getQueryRequests(int queryId);

}
