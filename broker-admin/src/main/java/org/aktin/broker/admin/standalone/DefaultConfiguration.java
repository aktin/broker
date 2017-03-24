package org.aktin.broker.admin.standalone;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class DefaultConfiguration implements Configuration{
	@Override
	public InputStream readAPIKeyProperties() throws FileNotFoundException {
		return new FileInputStream("api-keys.properties");
	}

	@Override
	public String getDatabasePath() {
		return "broker";
	}

	@Override
	public int getPort() {
		return 8080;
	}

	@Override
	public String getAggregatorDataPath() {
		return "aggregator-data";
	}
	@Override
	public String getBrokerDataPath() {
		return "broker-data";
	}

}
