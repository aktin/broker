package org.aktin.broker.client.live;


import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.xml.RequestStatus;

import lombok.extern.java.Log;


/**
 * Broker client process execution service.
 * @author R.W.Majeed
 *
 */
@Log
public abstract class CLIExecutionService<T extends AbortableRequestExecution> extends AbstractExecutionService<T> implements Runnable{
	private CLIClientPluginConfiguration<?> config;

	public CLIExecutionService(BrokerClient2 client, CLIClientPluginConfiguration<?> config ) {
		super(client);
		this.config = config;
		setExecutor(configureExecutor(config));
	}

	public void setConfiguration(CLIClientPluginConfiguration<?> config) {
		this.config = config;
		this.setExecutor(configureExecutor(config));
	}

	public static ScheduledExecutorService configureExecutor(CLIClientPluginConfiguration<?> config) {
		if( config.getExecutorThreads() < 1 ) {
			throw new IllegalArgumentException("Need at least one process executor thread.");
		}
		if( config.getExecutorThreads() == 1 ) {
			log.info("Executor uses singleThreadScheduledExecutor");
			return Executors.newSingleThreadScheduledExecutor();
		}else {
			log.log(Level.INFO, "Executor uses scheduledThreadPool({0})", Integer.valueOf(config.getExecutorThreads()));
			return Executors.newScheduledThreadPool(config.getExecutorThreads());
		}
	}

	@Override
	protected void onShutdown(List<T> unprocessedExecutions) {
		// ignore unprocessed executions
	}

	@Override
	protected void onStatusUpdate(T execution, RequestStatus status) {
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
	public void loadQueue() throws IOException {
		pollRequests();
	}

	@Override
	public int pollRequests() throws IOException {
		if( config.websocketReconnectPolling ) {
			log.info("polling for open requests");
			return super.pollRequests();			
		}else {
			return -1;
		}
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

	private void runWebsocketDisabledMode() {
		while( !isAborted() ) {
			try {
				int newRequests = super.pollRequests();
				if( newRequests > 0 ) {
					log.info("Polling returned "+newRequests+" new requests");
				}else {
					log.fine("Polling returned no new requests");
				}
			} catch (IOException e) {
				log.log(Level.WARNING,"Failed to poll for requests",e);
			}
			
			synchronized( this ) {
				try {
					this.wait(1000*config.getWebsocketReconnectSeconds());
				} catch (InterruptedException e) {
					log.info("Interrupted during wait between polling");
					;// we expected this interruption, e.g. during shutdown
				}
			}
		}
	}

	@Override
	public void run() {
		if( config.websocketDisabled == true ) {
			log.info("Websocket disabled. Using polling only with interval "+config.getWebsocketReconnectSeconds());
			runWebsocketDisabledMode();
			return;
		}
		// regular mode using websocket connection
		try {
			establishWebsocketConnection();
			log.info("websocket connection established");
			// try to load queue. this will do nothing if disabled by configuration
			loadQueue();
		} catch (IOException e) {
			log.severe("websocket connection failed: "+e.getMessage());
			// exit if the initial websocket connection fails?
			// for now, just continue trying to establish the connection
		}
		
		if( config.getWebsocketPingpongSeconds() != 0 ) {
			setWebsocketPingPongTimer(config.getWebsocketPingpongSeconds() * 1000);
			log.info("websocket ping-pong delay set to "+config.getWebsocketPingpongSeconds()+"s");
		}else{
			log.info("websocket ping-pong disabled");
		}
		
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
					establishWebsocketConnection();
					log.info("websocket connection re-established");
					// websocket established. poll for missed requests
					pollRequests();
				} catch (IOException e) {
					log.warning("websocket reconnect failed: "+e.getMessage());
				}
			}else {
				// websocket established, wait for something to happen
				// e.g. shutdown or websocket closing
				synchronized( this ) {
					try {
						this.wait();
					} catch (InterruptedException e) {
						;// we expected this interruption
					}
				}
			}
		}
	}
}
