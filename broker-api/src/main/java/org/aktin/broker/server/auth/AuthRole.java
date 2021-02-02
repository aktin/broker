package org.aktin.broker.server.auth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
	NODE_WRITE;

	public static final Set<AuthRole> ALL_NODE = new HashSet<>(Arrays.asList(AuthRole.NODE_READ, AuthRole.NODE_WRITE));
	public static final Set<AuthRole> ALL_ADMIN = new HashSet<>(Arrays.asList(AuthRole.ADMIN_READ, AuthRole.ADMIN_WRITE));
}
