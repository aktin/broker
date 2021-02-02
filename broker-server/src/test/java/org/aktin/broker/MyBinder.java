package org.aktin.broker;

import java.io.IOException;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.aktin.broker.auth.AuthCache;
import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.db.AggregatorImpl;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.db.BrokerImpl;
import org.aktin.broker.download.DownloadManager;
import org.aktin.broker.server.auth.HeaderAuthentication;
import org.aktin.broker.util.RequestTypeManager;
import org.glassfish.hk2.utilities.binding.AbstractBinder;


public class MyBinder extends AbstractBinder{

	private DataSource ds;
	private BrokerBackend backend;
	private AuthCache cache;
	private HeaderAuthentication headerAuth;

	public MyBinder(DataSource ds, HeaderAuthentication headerAuth) throws IOException{
		this.ds = ds;
		this.headerAuth = headerAuth;
		this.backend = new BrokerImpl(ds, Paths.get("target/broker-data"));
		this.cache = new AuthCache(backend);
	}
	public AuthCache getAuthCache() {
		return cache;
	}
	@Override
	protected void configure() {
//		bind(Impl.class).to(Inter.class);
		// singleton

		try {
			// download manager
			DownloadManager downloads = new DownloadManager();
			downloads.setTempDirectory(Paths.get("target/download-temp"));
			bind(downloads).to(DownloadManager.class);

			// broker
			backend.clearDataDirectory();

			bind(backend).to(BrokerBackend.class);
			bind(cache).to(AuthCache.class);
			
			// aggregator
			AggregatorImpl adb = new AggregatorImpl(ds, Paths.get("target/aggregator-data"));
			// clear uploaded files
			adb.clearDataDirectory();
			bind(adb).to(AggregatorBackend.class);
			// try to bind HeaderAuthenticator
			bind(headerAuth).to(HeaderAuthentication.class);
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		bind(new RequestTypeManager()).to(RequestTypeManager.class);

		//bind(PMService.class).to(AbstractCell.class);
		//bind(WorkplaceService.class).to(AbstractCell.class);
	}

}
