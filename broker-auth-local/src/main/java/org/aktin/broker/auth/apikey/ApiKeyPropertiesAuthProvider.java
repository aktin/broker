package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.aktin.broker.server.auth.AbstractAuthProviderFactory;

public class ApiKeyPropertiesAuthProvider extends AbstractAuthProviderFactory {
	private PropertyFileAPIKeys keys;

	public ApiKeyPropertiesAuthProvider() {
		keys = null; // lazy init, load later in getInstance by using supplied path
	}
	public ApiKeyPropertiesAuthProvider(InputStream in) throws IOException {
		this.keys = new PropertyFileAPIKeys(in);
	}


	@Override
	public PropertyFileAPIKeys getInstance() throws IOException {
		if( this.keys == null ) {
			// not previously loaded
			try( InputStream in = Files.newInputStream(path.resolve("api-keys.properties")) ){
				this.keys = new PropertyFileAPIKeys(in);
			}
		}else {
			;// already loaded, use existing one
		}
		return keys;
//		if( System.getProperty("rewriteNodeDN") != null ){
//			int count = BrokerImpl.updatePrincipalDN(ds, keys.getMap());
//			// output/log what happened, use count returned from above method
//			System.out.println("Rewritten "+count+" node DN strings.");
//		}
	}
}
