package org.aktin.broker.client.live.noop;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.aktin.broker.client.live.CLIClientPluginConfiguration;
import org.aktin.broker.client2.BrokerClient2;

public class NoopExecutionPlugin extends CLIClientPluginConfiguration<NoopExecutionService>{

	public NoopExecutionPlugin(InputStream in) throws IOException {
		super(in);
	}

	@Override
	protected void loadConfig(Properties properties) {
		// load any extra configuration parameters here.
	}

	@Override
	protected NoopExecutionService createService(BrokerClient2 client) {
		return new NoopExecutionService(client, this);
	}

}
