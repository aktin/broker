package org.aktin.broker.client.live.sysproc;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.aktin.broker.client.live.AbstractExecutionService;
import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.xml.RequestStatus;

import lombok.extern.java.Log;


/**
 * Broker client process execution service.
 * @author R.W.Majeed
 *
 */
@Log
public class ProcessExecutionService extends AbstractExecutionService<ProcessExecution> implements Runnable{
	private ProcessExecutionConfig config;

	public ProcessExecutionService(BrokerClient2 client, ProcessExecutionConfig config) {
		super(client, Executors.newSingleThreadExecutor());
		this.config = config;
	}

	public ProcessExecutionService(BrokerClient2 client, ProcessExecutionConfig config, ExecutorService executor) {
		super(client, executor);
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
		if( status == RequestStatus.failed && execution.getCause() != null ) {
			if( execution.isAborted() ) {
				log.warning("aborted "+execution.getRequestId());
			}else {
				// print exception
				execution.getCause().printStackTrace();
				System.err.println();
			}
		}
		log.info("status "+execution.getRequestId()+" -> "+status.toString());
	}

	@Override
	public void loadQueue() {
		// no resume of previous requests
	}

	@Override
	protected void onWebsocketClosed(int status) {
		if( !isAborted() ) {
			// websocket closed by server
			log.warning("websocket closed "+status);
			synchronized( this ) {
				this.notifyAll();
			}
		}
	}

	@Override
	public void run() {
		try {
			startupWebsocketListener();
		} catch (IOException e) {
			log.severe("websocket connection failed: "+e.getMessage());
			// exit if the initial websocket connection fails?
			// for now, just continue trying to establish the connection
		}
		log.info("websocket connection established");
		long previousConnection = System.currentTimeMillis();

		// nothing to do, wait for exit
		while( !isAborted() ) {

			if( isWebsocketClosed() ) {
				// websocket closed by server
				// first try to re-establish the connection immediately. if this fails <= reconnectSeconds, then wait for requested delay
				if( config.getWebsocketReconnectSeconds() == -1 ) {
					log.info("websocket retry disabled. exiting.");
					shutdown();
					break;
				}
				long currentAttempt = System.currentTimeMillis();
				if( currentAttempt - previousConnection < config.getWebsocketReconnectSeconds()*1000 ) {
					// we need to wait the specified amout before next retry
					log.info("websocket retry after "+config.getWebsocketReconnectSeconds()+"s");
					try {
						Thread.sleep(1000*config.getWebsocketReconnectSeconds());
					} catch (InterruptedException e) {
						// interrupted during sleep, try again..
						continue;
					}
				}else {
					// immediate retry
				}
				try {
					previousConnection = System.currentTimeMillis();
					startupWebsocketListener();
					log.info("websocket connection re-established");
					// websocket established. poll for missed requests
					pollRequests();
				} catch (IOException e) {
					log.warning("websocket connection failed: "+e.getMessage());
				}
			}else {
				// websocket established, wait for something to happen
				// e.g. shutdown or websocket closing
				synchronized( this ) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						// we expected this interruption
					}
				}
			}
		}
	}
}
