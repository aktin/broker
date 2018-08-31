package org.aktin.broker.request;

/**
 * Enumeration of possible request states at the local data warehouse.
 * These local states are of finer granularity compared to the 
 * @author R.W.Majeed
 *
 */
public enum RequestStatus {
	/** Request was retrieved from the broker.
	 * This is the first status in the data warehouse.
	 */
	Retrieved,
	/** A user has opened the request to review it. */
	Seen,
	/** The request has been rejected (manually or automatically by a rule).
	 * No further processing is performed. A rejection can follow either after {@link #Seen}
	 * or manually after {@link #Completed}.
	 */
	Rejected,
	/** Request was queued for processing. It is waiting for access to resources. Further
	 * processing is done automatically.
	 */
	Queued,
	/** Request is currently being processed. */
	Processing,
	/** Processing has completed without errors and the results were stored.
	 * User interaction may occur at this point to review/verify the results 
	 * before submission. */
	Completed,
	/** Results are being transferred to the aggregator / remote endpoint. */
	Sending,
	/** Request results have been submitted. 
	 * This is a terminal status which should not be changed. */
	Submitted,
	/** Unexpected failure occurred at some point during processing or transfer. */
	Failed,
	/** The request was closed by the broker or deleted from it. */
	Expired
}
