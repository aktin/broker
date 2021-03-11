package org.aktin.broker.auth.openid;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.Setter;
import org.aktin.broker.server.auth.AbstractAuthProvider;
import org.aktin.broker.server.auth.HeaderAuthentication;

public class OpenIdAuthProvider extends AbstractAuthProvider{
	@Setter
	private OpenIdConfig config;
	private OpenIdAuthenticator auth;

	public OpenIdAuthProvider() {
		config = null; // lazy init, load later in getInstance by using supplied path
	}

	public OpenIdAuthProvider(InputStream in) {
		this.config = new OpenIdConfig(in);
	}
	private void loadConfig() throws IOException {
		if( this.config != null ) {
			// don't load if configuration already available, e.g. via setter method
			return;
		}
		// initialize configuration from config file in this.path
		try (InputStream in = Files.newInputStream(path.resolve("openid-config.properties"))) {
			this.config = new OpenIdConfig(in);
		}
	}
	@Override
	public HeaderAuthentication getInstance() throws IOException {
		if( this.auth == null ) {
			loadConfig();
			this.auth = new OpenIdAuthenticator(this.config);
		}
		return new OpenIdAuthenticator(config);
	}

}
