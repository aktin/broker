package org.aktin.broker.server.auth;

import java.nio.file.Path;

public abstract class AbstractAuthProvider implements AuthProvider{
	protected Path path;

	@Override
	public void setBasePath(Path path) {
		this.path = path;
	}


}
