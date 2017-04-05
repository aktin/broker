package org.aktin.broker.request;

import java.util.List;

/**
 * Request manager interface for a single broker.
 *
 * @author R.W.Majeed
 *
 */
public interface RequestManager {

	List<? extends RetrievedRequest> getRequests();

	default RetrievedRequest getRequest(int id){
		for( RetrievedRequest r : getRequests() ){
			if( r.getRequestId() == id ){
				return r;
			}
		}
		return null;
	}
	/**
	 * Get requests associated with the specified query.
	 * All returned requests belong to the same query id.
	 * @param queryId query id
	 * @return requests belonging to the specified query id
	 */
	List<? extends RetrievedRequest> getQueryRequests(int queryId);

}
