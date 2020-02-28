package org.aktin.broker.auth;

import java.util.function.Function;

public interface HeaderAuthentication {

	public Principal authenticateByHeaders(Function<String,String> getHeader);

}
