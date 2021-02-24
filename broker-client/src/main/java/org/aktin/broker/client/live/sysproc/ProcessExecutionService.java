package org.aktin.broker.client.live.sysproc;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import org.aktin.broker.client.live.AbstractExecutionService;
import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.xml.RequestStatus;


/**
 * Broker client process execution service.
 * @author R.W.Majeed
 *
 */
public class ProcessExecutionService extends AbstractExecutionService<ProcessExecution>{
	private ProcessExecutionConfig config;

	public ProcessExecutionService(BrokerClient2 client, ProcessExecutionConfig config) throws IOException {
		super(client, Executors.newSingleThreadExecutor());
		this.config = config;
	}

	@Override
	protected ProcessExecution initializeExecution(Integer requestId) {
		return new ProcessExecution(requestId, config);
	}

	@Override
	protected void onShutdown(List<ProcessExecution> unprocessedExecutions) {
		// ignore unprocessed executions
	}

	@Override
	protected void onStatusUpdate(ProcessExecution execution, RequestStatus status) {
		if( status == RequestStatus.failed ) {
			// print exception
		}
	}

	@Override
	public void loadQueue() {
		// no resume of previous requests
	}
}
