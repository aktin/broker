package org.aktin.broker.client2.validator;

/**
 * Custom request inspection and validation via this interface.
 * The client will not reuse the objects returned by {@link #getValidator(String)}. 
 * Implementations may decide whether to share/reuse instances.
 * 
 * @author R.W.Majeed
 *
 */
public interface RequestValidatorFactory {
	// TODO allow retrieval of configuration names or declare them
	// TODO allow setting configuration 

	/**
	 * Return a validation instance for the specified media type. This method will be called once for 
	 * each request to be validated. 
	 * @param mediaType MIME media type
	 * @return instance to be used for request validation. Must be non-null.
	 * @throws IllegalArgumentException Specified mediatype not supported e.g. with additional information.
	 */
	RequestValidator getValidator(String mediaType) throws IllegalArgumentException;
}
