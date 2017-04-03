package org.aktin.broker.request;

import java.time.Instant;

public interface ActionLogEntry {
	/** User who triggered the action.
	 * May be {@code null} for automatic actions.
	 * @return user id or {@code null}
	 */
	String getUserId();
	/**
	 * Timestamp when the action was triggered.
	 * @return timestamp
	 */
	Instant getTimestamp();
	RequestStatus getOldStatus();
	RequestStatus getNewStatus();
	/**
	 * Optional description for the action.
	 * The description may be automatically generated
	 * (e.g. error log for failures) or user specified
	 * (e.g. reason for rejection).
	 * @return description or {@code null}.
	 */
	String getDescription();
}
