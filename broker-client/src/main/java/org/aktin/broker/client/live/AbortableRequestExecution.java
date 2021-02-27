package org.aktin.broker.client.live;

import java.util.function.BooleanSupplier;

import lombok.Setter;
public abstract class AbortableRequestExecution extends AbstractRequestExecution {
	
	@Setter
	private BooleanSupplier globalAbort;
	private volatile boolean localAbort;
	
	public AbortableRequestExecution(int requestId) {
		super(requestId);
		this.localAbort = false;
		// default to no global abortion
		this.globalAbort = () -> false;
	}

	/**
	 * Sets the local abort flag to true.
	 */
	public void abortLocally() {
		this.localAbort = true;
	}

	/**
	 * Check whether a local or global abort is flagged.
	 * @return true iif local abort or global abort is true
	 */
	public boolean isAborted() {
		return localAbort || globalAbort.getAsBoolean();
	}

	
}
