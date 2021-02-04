package org.aktin.broker.auth.openid;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.Setter;
import org.aktin.broker.server.auth.AbstractAuthProvider;
import org.aktin.broker.server.auth.HeaderAuthentication;

@Setter
public class OpenIdAuthProvider extends AbstractAuthProvider{
	private OpenIdConfig config;

	public OpenIdAuthProvider(InputStream in) {
		this.config = new OpenIdConfig(in);
	}
	public void loadConfig() throws IOException {
		// TODO initialize configuration from config file in this.path
		try (InputStream in = Files.newInputStream(path.resolve("openid-config.properties"))) {
			this.config = new OpenIdConfig(in);
		}
	}
	@Override
	public HeaderAuthentication getInstance() throws IOException {
		if( this.config == null ) {
			loadConfig();
		}
		return new OpenIdAuthenticator(config);
	}

}
