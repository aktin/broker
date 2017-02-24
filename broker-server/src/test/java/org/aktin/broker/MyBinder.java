package org.aktin.broker;

import java.io.IOException;
import java.nio.file.Paths;

import javax.sql.DataSource;

import org.aktin.broker.auth.AuthCache;
import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.db.AggregatorImpl;
import org.aktin.broker.db.BrokerBackend;
import org.aktin.broker.db.BrokerImpl;
import org.glassfish.hk2.utilities.binding.AbstractBinder;


public class MyBinder extends AbstractBinder{

	private DataSource ds;
	
	public MyBinder(DataSource ds){
		this.ds = ds;
	}
	@Override
	protected void configure() {
//		bind(Impl.class).to(Inter.class);
		// singleton

		try {
			// broker
			BrokerBackend backend = new BrokerImpl(ds, Paths.get("target/broker-data"));
			backend.clearDataDirectory();
			bind(backend).to(BrokerBackend.class);
			bind(new AuthCache(backend)).to(AuthCache.class);
			// aggregator
			AggregatorImpl adb = new AggregatorImpl(ds, Paths.get("target/aggregator-data"));
			// clear uploaded files
			adb.clearDataDirectory();
			bind(adb).to(AggregatorBackend.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		bind(new RequestTypeManager()).to(RequestTypeManager.class);

		//bind(PMService.class).to(AbstractCell.class);
		//bind(WorkplaceService.class).to(AbstractCell.class);
	}

}
