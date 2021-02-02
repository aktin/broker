package org.aktin.broker.admin.standalone;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.aktin.broker.server.auth.AuthProviderFactory;

public class DefaultConfiguration implements Configuration{
	@Override
	public int getPort() {
		return 8080;
	}

//	@Override
//	public String getDatabasePath() {
//		return "broker";
//	}
//	@Override
//	public String getAggregatorDataPath() {
//		return "aggregator-data";
//	}
//	@Override
//	public String getBrokerDataPath() {
//		return "broker-data";
//	}
//
//	@Override
//	public String getTempDownloadPath() {
//		return "download-temp";
//	}

	@Override
	public AuthProviderFactory getAuthProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getBasePath() {
		return Paths.get(".");
	}

}
