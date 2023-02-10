package org.aktin.broker.client.live.sysproc;

import org.aktin.broker.client2.BrokerClient2;

public class ProcessExecutionPlugin extends CLIExecutionService<ProcessExecution>{
	private ProcessExecutionConfig config;

	public ProcessExecutionPlugin(BrokerClient2 client, ProcessExecutionConfig config) {
		super(client, config);
		this.config = config;
	}

	@Override
	protected ProcessExecution initializeExecution(Integer requestId) {
		return new ProcessExecution(requestId, config);
	}

}
