package org.aktin.broker.auth;

import java.util.List;
import java.util.Map;

public interface HeaderAuthentication {

	public String[] authenticateByHeaders(Map<String,List<String>> headers);

}
