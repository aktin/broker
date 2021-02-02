package org.aktin.broker.admin.standalone;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.aktin.broker.auth.AuthCache;
import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.db.AggregatorImpl;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.db.BrokerImpl;
import org.aktin.broker.download.DownloadManager;
import org.aktin.broker.server.auth.AuthProviderFactory;
import org.aktin.broker.util.RequestTypeManager;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.binding.ScopedBindingBuilder;



public class MyBinder extends AbstractBinder{
	private static final Logger log = Logger.getLogger(MyBinder.class.getName());

	private DataSource ds;
	private Configuration config;
	private BrokerBackend broker;
	private AggregatorBackend aggregator;
	private DownloadManager downloads;
	private AuthProviderFactory authProvider;
	private AuthCache authCache;
	private List<Closeable> closeables;
	
	public MyBinder(DataSource ds,Configuration config, AuthProviderFactory authProvider) throws IOException{
		this.ds = ds;
		this.authProvider = authProvider;
		this.config = config;
		closeables = new LinkedList<>();
		this.broker = new BrokerImpl(ds, Paths.get(config.getBrokerDataPath()));
		this.authCache = new AuthCache(broker);
	}
	@SuppressWarnings("unchecked")
	@Override
	protected void configure() {
		// singleton

		try {
			// set aggregator data directory
			aggregator = new AggregatorImpl(ds, Paths.get(config.getAggregatorDataPath()));
			// download manager
			downloads = new DownloadManager(Paths.get(config.getTempDownloadPath()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		bind(broker).to(BrokerBackend.class);
		bind(aggregator).to(AggregatorBackend.class);
		bind(authCache).to(AuthCache.class);
		bind(new RequestTypeManager()).to(RequestTypeManager.class);
		bind(downloads).to(DownloadManager.class);
		
		// bind auth provider singletons if available
		authProvider.bindSingletons( (o,c) -> {
			bind(o).to((Class<? super Object>) c);
		});
		
	}
	@Override
	public <T> ScopedBindingBuilder<T> bind(T service) {
		if( service instanceof Closeable ) {
			closeables.add((Closeable)service);
		}
		return super.bind(service);
	}

	public void closeCloseables() {
		for( Closeable c : closeables ) {
			try {
				c.close();
			} catch (IOException e) {
				log.log(java.util.logging.Level.WARNING, "Exception during closing of "+c.getClass(), e);
			}
		}
	}

	public AuthCache getAuthCache() {return authCache;}
}
