package org.aktin.broker.client.live.noop;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.aktin.broker.client.live.CLIClientPluginConfiguration;
import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.validator.RequestValidatorFactory;

import lombok.Getter;

@Getter
public class NoopExecutionPlugin extends CLIClientPluginConfiguration<NoopExecutionService>{

	private RequestValidatorFactory requestValidation;

	public NoopExecutionPlugin(InputStream in) throws IOException {
		super(in);
	}

	@Override
	protected void loadConfig(Properties properties) throws IOException{
		// load any extra configuration parameters here.
		this.requestValidation = loadValidatorFactory(properties);
	}

	@Override
	protected NoopExecutionService createService(BrokerClient2 client) {
		return new NoopExecutionService(client, this);
	}

}
