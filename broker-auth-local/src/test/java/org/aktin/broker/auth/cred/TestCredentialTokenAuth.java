package org.aktin.broker.auth.cred;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.aktin.broker.auth.HeaderUtils;
import org.aktin.broker.server.auth.AuthInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestCredentialTokenAuth {
	private CredentialTokenAuth auth;
	private TokenManager manager;
	private String password;

	@Before
	public void initializeInstanceFromProvider() throws IOException {
		this.password = TokenManager.randomPassword();
		CredentialTokenAuthProvider prov = new CredentialTokenAuthProvider(password);
		prov.setBasePath(Paths.get("src/test/resources"));
		auth = prov.getInstance();
		manager = prov.getManager();
	}
	@Test
	public void validAuthHeaderShouldReturnValidAuth() throws IOException {
		// set headers
		Token t = manager.authenticate("admin", password.toCharArray());
		
		Map<String,String> headers = new HashMap<>();
		HeaderUtils.putAuthorizationBearerHeader(headers, t.getGUID());
		// 
		AuthInfo info = auth.authenticateByHeaders(headers::get);
		Assert.assertNotNull(info);
		Assert.assertEquals("admin", info.getUserId());
	}

	// TODO token invalidation is not implemented yet
	//@Test
	public void invalidatedTokenShouldNotAuthenticate() throws IOException {
		// set headers
		Token t = manager.authenticate("admin", password.toCharArray());
		
		Map<String,String> headers = new HashMap<>();
		HeaderUtils.putAuthorizationBearerHeader(headers, t.getGUID());

		// first expect successful auth 
		AuthInfo info = auth.authenticateByHeaders(headers::get);
		Assert.assertNotNull(info);

		// invalidate token
		t.invalidate();
		info = auth.authenticateByHeaders(headers::get);
		// expect auth failure
		Assert.assertNull(info);
	}
}
