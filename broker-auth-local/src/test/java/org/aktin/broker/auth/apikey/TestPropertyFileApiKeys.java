package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.aktin.broker.auth.HeaderUtils;
import org.aktin.broker.server.auth.AuthInfo;
import org.junit.Assert;
import org.junit.Test;

public class TestPropertyFileApiKeys {
	private static final String FIRST_API_KEY = "xxxApiKey123";
	private static final String FIRST_CLIENT_DN = "CN=Schwarzwaldklinik,O=Schwarzwaldklinik,L=Schwarzwald";
	private static final String SECOND_API_KEY = "xxxApiKey567";
	@Test
	public void getInstanceFromProvider() throws IOException {
		ApiKeyPropertiesAuthProvider prov = new ApiKeyPropertiesAuthProvider();
		prov.setBasePath(Paths.get("src/test/resources"));
		PropertyFileAPIKeys ak = prov.getInstance();
		// make sure the properties are loaded
		Assert.assertNotNull(ak.lookupAuthInfo(FIRST_API_KEY));
	}

	private PropertyFileAPIKeys loadInstance() throws IOException {
		try( InputStream in = getClass().getResourceAsStream("/api-keys.properties") ){
			return new PropertyFileAPIKeys(in);
		}
	}
	@Test
	public void validAuthHeaderShouldReturnValidAuth() throws IOException {
		PropertyFileAPIKeys ak = loadInstance();
		// set headers
		Map<String,String> headers = new HashMap<>();
		HeaderUtils.putAuthorizationBearerHeader(headers, FIRST_API_KEY);
		//
		AuthInfo info = ak.authenticateByHeaders(headers::get);
		Assert.assertNotNull(info);
		Assert.assertEquals(FIRST_CLIENT_DN, info.getClientDN());
	}
	@Test
	public void inactiveAuthHeaderShouldReturnNullAuth() throws IOException {
		PropertyFileAPIKeys ak = loadInstance();
		Map<String,String> headers = new HashMap<>();
		HeaderUtils.putAuthorizationBearerHeader(headers, SECOND_API_KEY);
		AuthInfo info = ak.authenticateByHeaders(headers::get);
		Assert.assertNull(info);
	}
	@Test
	public void missingHeaderShouldReturnNullAuth() throws IOException {
		PropertyFileAPIKeys ak = loadInstance();
		// set headers
		Map<String,String> headers = new HashMap<>();
		headers.put("Bla", "bla");
		//
		AuthInfo info = ak.authenticateByHeaders(headers::get);
		// no auth header should the auth to return null
		Assert.assertNull(info);
	}
	@Test
	public void wrongAuthTokenShouldReturnNullAuth() throws IOException {
		PropertyFileAPIKeys ak = loadInstance();
		// set headers
		Map<String,String> headers = new HashMap<>();
		// intentionally use wrong token
		HeaderUtils.putAuthorizationBearerHeader(headers, "x"+FIRST_API_KEY);
		//
		AuthInfo info = ak.authenticateByHeaders(headers::get);
		Assert.assertNull(info);
	}
}
