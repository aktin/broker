package org.aktin.broker.request;

public enum QueryRuleAction {
	/**
	 * reject matching queries
	 */
	REJECT, 
	/**
	 * accept execution of matching queries, 
	 * but require interaction before results are submitted
	 */
	ACCEPT_EXECUTE, 
	/**
	 * accept execution of matching queries and automatically
	 * submit the result data.
	 */
	ACCEPT_SUBMIT
}
