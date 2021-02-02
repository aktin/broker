package org.aktin.broker.auth.openid;

import java.io.IOException;
import org.aktin.broker.server.auth.AbstractAuthProvider;
import org.aktin.broker.server.auth.HeaderAuthentication;

public class OpenIdAuthProvider extends AbstractAuthProvider{
	private OpenIdConfig config;

	public void setConfig(OpenIdConfig config) {
		this.config = config;
	}
	public void loadConfig() {
		// TODO initialize configuration from config file in this.path
		this.config = new OpenIdConfig();
	}
	@Override
	public HeaderAuthentication getInstance() throws IOException {
		if( this.config == null ) {
			loadConfig();
		}
		// TODO load configuration
		return null;
	}

}
