package org.aktin.broker.request;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Request manager interface for a single broker.
 *
 * @author R.W.Majeed
 *
 */
public interface RequestManager {

	List<? extends RetrievedRequest> getRequests();

	default Stream<? extends RetrievedRequest> requests(){
		return getRequests().stream();
	}

	default RetrievedRequest getRequest(int id){
		for( RetrievedRequest r : getRequests() ){
			if( r.getRequestId() == id ){
				return r;
			}
		}
		return null;
	}

	InteractionPreset getInteractionPreset();

	/**
	 * Get requests associated with the specified query.
	 * All returned requests belong to the same query id.
	 * @param queryId query id
	 * @return requests belonging to the specified query id
	 */
	default Stream<? extends RetrievedRequest> getQueryRequests(int queryId){
		return requests().filter( request -> {
			Integer id = request.getRequest().getQueryId();
			return id != null && id.intValue() == queryId;
		});
	}

	/**
	 * Iterate over query rules. Includes default rules which have a {@code null} queryId.
	 * @param action iteration consume
	 */
	void forEachRule(Consumer<BrokerQueryRule> action);
	
	BrokerQueryRule getQueryRule(int queryId);
	BrokerQueryRule getDefaultRule();

	BrokerQueryRule createDefaultRule(String userId, QueryRuleAction action)throws IOException;
	BrokerQueryRule createQueryRule(RetrievedRequest request, String userId, QueryRuleAction action)throws IOException;
	/**
	 * Delete the query rule with the given query id
	 * @param queryId query id or {@code null} for default rule
	 * @throws FileNotFoundException no rule found with the given queryId
	 * @throws IOException other IO error
	 */
	void deleteQueryRule(Integer queryId)throws FileNotFoundException, IOException;

	
}
