package org.aktin.broker.auth.cred;

import org.junit.Assert;
import org.junit.Test;

public class TestTokenManager {

	@Test
	public void authRandomPassword() {
		String pw = TokenManager.randomPassword();
		TokenManager m = new TokenManager(pw);
		// auth should succeed
		Token t = m.authenticate("admin", pw.toCharArray());
		Assert.assertNotNull(t);
		Assert.assertEquals("admin", t.getName());
		// auth with wrong password should fail
		t = m.authenticate("admin", (pw+"x").toCharArray());
		Assert.assertNull(t);
	}


	@Test
	public void systemPropertyPassword() {
		String pw = "mkl23dskl2"; // TODO change to random string
		System.setProperty(TokenManager.PROPERTY_BROKER_PASSWORD, pw);
		TokenManager m = new TokenManager();
		Token t = m.authenticate("admin", pw.toCharArray());
		// clear property
		System.clearProperty(TokenManager.PROPERTY_BROKER_PASSWORD);
		Assert.assertNotNull(t);
	}
}
