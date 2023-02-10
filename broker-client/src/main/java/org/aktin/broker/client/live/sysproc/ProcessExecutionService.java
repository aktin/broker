package org.aktin.broker.client.live.sysproc;

import org.aktin.broker.client.live.CLIExecutionService;
import org.aktin.broker.client2.BrokerClient2;

public class ProcessExecutionService extends CLIExecutionService<ProcessExecution>{
	private ProcessExecutionPlugin config;

	public ProcessExecutionService(BrokerClient2 client, ProcessExecutionPlugin config) {
		super(client, config);
		this.config = config;
	}

	@Override
	protected ProcessExecution initializeExecution(Integer requestId) {
		return new ProcessExecution(requestId, config);
	}

}
