package org.aktin.broker.xml;

public enum RequestStatus {
	// TODO refactor to uppercase constant names
	/** The request has been retrieved by the node. */
	retrieved,
	/** The request is waiting for user interaction,
	 * e.g. manual confirmation of execution or result submission.
	 */
	interaction,
	/** The request was accepted for processing, but is currently waiting in the execution queue. XXX rename to queued */
	queued,
	/** The request is currently being processed.  */
	processing,
	/** The request completed successfully and results have been submitted */
	completed,
	/** The request was rejected and will not be processed */
	rejected,
	/** An error occurred during request processing at the node */
	failed,
	/** The request was closed by the broker or deleted from it. */
	expired;
}
