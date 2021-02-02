package org.aktin.broker.server.auth;

import java.nio.file.Path;

public abstract class AbstractAuthProviderFactory implements AuthProviderFactory{
	protected Path path;

	@Override
	public void setBasePath(Path path) {
		this.path = path;
	}


}
