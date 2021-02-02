package org.aktin.broker.auth;

import java.util.Map;

public class HeaderUtils {
	public static void putAuthorizationBearerHeader(Map<String,String> headers, String token) {
		headers.put("Authorization", "Bearer "+token);
	}

}
