package org.aktin.broker.auth;

import java.util.function.Function;

public interface HeaderAuthentication {

	Principal authenticateByHeaders(Function<String,String> getHeader);

}
