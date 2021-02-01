package org.aktin.broker.server.auth;

public enum AuthRole {
	/**
	 * List all requests, list all node info, download result data, etc.
	 */
	ADMIN_READ,
	/** Create requests, delete requests, etc. */
	ADMIN_WRITE,
	/**
	 * Read access from the node perspective. E.g. retrieve published requests
	 */
	NODE_READ,
	/**
	 * Write access from the node perspective. E.g. submit calculated results for a published request, update own status, etc.
	 */
	NODE_WRITE
}
