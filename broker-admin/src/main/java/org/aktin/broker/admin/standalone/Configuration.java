package org.aktin.broker.admin.standalone;

import java.io.IOException;
import java.io.InputStream;

public interface Configuration {

	// TODO add configuration to specify external class implementing HeaderAuthentication
	InputStream readAPIKeyProperties()throws IOException;
	String getDatabasePath();
	String getAggregatorDataPath();
	String getBrokerDataPath();
	String getTempDownloadPath();
	int getPort();
}
