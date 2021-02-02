package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.aktin.broker.server.auth.AbstractAuthProviderFactory;

public class ApiKeyPropertiesAuthProvider extends AbstractAuthProviderFactory {

	@Override
	public PropertyFileAPIKeys getInstance() throws IOException {

		PropertyFileAPIKeys auth;
		try( InputStream in = Files.newInputStream(path.resolve("api-keys.properties")) ){
			auth = new PropertyFileAPIKeys(in);
		}
//		if( System.getProperty("rewriteNodeDN") != null ){
//			int count = BrokerImpl.updatePrincipalDN(ds, keys.getMap());
//			// output/log what happened, use count returned from above method
//			System.out.println("Rewritten "+count+" node DN strings.");
//		}

		return auth;
	}
}
