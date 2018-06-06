package org.aktin.broker.admin.standalone;

import java.io.IOException;
import java.io.InputStream;

public interface Configuration {

	public InputStream readAPIKeyProperties()throws IOException;
	public String getDatabasePath();
	public String getAggregatorDataPath();
	public String getBrokerDataPath();
	public String getTempDownloadPath();
	public int getPort();
}
