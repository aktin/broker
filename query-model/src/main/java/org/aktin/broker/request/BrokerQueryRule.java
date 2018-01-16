package org.aktin.broker.request;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import org.aktin.broker.query.xml.QueryRequest;

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
	

	/**
	 * Verify the signature with the query request.
	 * @param request request to verify the signature for
	 * @return {@code true} for a matching signature, {@code false} otherwise.
	 * @throws NoSuchAlgorithmException signature algorithm not supported by the JRE
	 * @throws IOException IO error during signature calculation
	 */
	public boolean verifySignature(QueryRequest request) throws NoSuchAlgorithmException, IOException;
}
