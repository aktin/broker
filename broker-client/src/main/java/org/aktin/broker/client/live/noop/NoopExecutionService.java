package org.aktin.broker.client.live.noop;

import org.aktin.broker.client.live.CLIExecutionService;
import org.aktin.broker.client2.BrokerClient2;

public class NoopExecutionService extends CLIExecutionService<NoopExecution>{
	private NoopExecutionPlugin config;

	public NoopExecutionService(BrokerClient2 client, NoopExecutionPlugin config) {
		super(client, config);
	}

	@Override
	protected NoopExecution initializeExecution(Integer requestId) {
		return new NoopExecution(requestId, config);
	}

}
