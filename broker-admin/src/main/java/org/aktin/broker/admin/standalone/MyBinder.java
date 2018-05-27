package org.aktin.broker.admin.standalone;

import java.io.IOException;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.aktin.broker.RequestTypeManager;
import org.aktin.broker.admin.auth.TokenManager;
import org.aktin.broker.auth.AuthCache;
import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.db.AggregatorImpl;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.db.BrokerImpl;
import org.aktin.broker.download.DownloadManager;
import org.glassfish.hk2.utilities.binding.AbstractBinder;


public class MyBinder extends AbstractBinder{

	private DataSource ds;
	private Configuration config;
	private BrokerBackend broker;
	private AggregatorBackend aggregator;
	
	public MyBinder(DataSource ds,Configuration config){
		this.ds = ds;
		this.config = config;
	}
	@Override
	protected void configure() {
		// singleton

		try {
			broker = new BrokerImpl(ds, Paths.get(config.getBrokerDataPath()));
			// set aggregator data directory
			aggregator = new AggregatorImpl(ds, Paths.get(config.getAggregatorDataPath()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		bind(broker).to(BrokerBackend.class);
		bind(aggregator).to(AggregatorBackend.class);
		bind(new AuthCache(broker)).to(AuthCache.class);
		bind(new RequestTypeManager()).to(RequestTypeManager.class);
		bind(new DownloadManager()).to(DownloadManager.class);

		bind(new TokenManager()).to(TokenManager.class);
		// bind 
		//bind(PMService.class).to(AbstractCell.class);
		//bind(WorkplaceService.class).to(AbstractCell.class);
	}

}
