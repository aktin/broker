package org.aktin.broker.xml;

public enum RequestStatus {
	// TODO refactor to uppercase constant names
	/** The request has been retrieved by the node. */
	retrieved,
	/** The request was accepted for processing, but is currently waiting in the execution queue. XXX rename to queued */
	accepted,
	/** The request is currently being processed.  */
	processing,
	/** The request completed successfully and results have been submitted */
	completed, 
	/** The request was rejected. */
	rejected,
	/** An error occurred during request processing at the node */
	failed; 
}
