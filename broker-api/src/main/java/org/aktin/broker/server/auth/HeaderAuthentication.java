package org.aktin.broker.server.auth;

import java.io.IOException;
import java.util.function.Function;


/**
 * Interface for using HTTP header for authentication. Implementations should use/inject
 * an instance of {@link AuthCache} to retrieve the {@link Principal} needed for return of
 * {@link #authenticateByHeaders(Function)}.
 *
 * @author R.W.Majeed
 *
 */
public interface HeaderAuthentication {

	/**
	 * Authenticate a user using information from HTTP headers
	 *
	 * @param getHeader function to retrieve HTTP headers
	 * @return principal for successful authentication. If authentication failed, {@code null} should
	 * be returned.
	 *
	 * @throws IOException Unexpected communication errors during authentication. Don't throw exceptions for access-denied - instead return {@code null}.
	 */
	AuthInfo authenticateByHeaders(Function<String,String> getHeader) throws IOException;

}
