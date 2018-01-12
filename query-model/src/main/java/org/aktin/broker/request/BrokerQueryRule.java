package org.aktin.broker.request;

import java.time.Instant;

public interface BrokerQueryRule {

	/**
	 * Query ID to which the rule applies
	 * @return query id or {@code null} for master rules applying to all queries
	 */
	public Integer getQueryId();
	/**
	 * Retrieve the id of the user who created the rule
	 * @return user id
	 */
	public String getUserId();
	/**
	 * Timestamp when the rule was created
	 * @return timestamp
	 */
	public Instant getTimestamp();
	/**
	 * Signature algorithm used to create the signature in {@link #getSignatureData()}
	 * @return Java message digest algorithm or signature algorithm
	 */
	public String getSignatureAlgorithm();
	public byte[] getSignatureData();

	public QueryRuleAction getAction();
	
}
