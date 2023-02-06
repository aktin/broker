package org.aktin.broker.client2.validator;

/**
 * Exception indicating that request content was not considered valid.
 */
public class ValidationError extends Exception{
	private static final long serialVersionUID = 1L;

	public ValidationError(String message, Throwable cause) {
		super(message,cause);
	}
	public ValidationError(Throwable cause) {
		super(cause);
	}
}
