package org.aktin.broker.client.live;

import java.io.Closeable;
import java.io.IOException;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.client2.NotificationListener;
import org.aktin.broker.xml.RequestStatus;

public abstract class AbstractExecutionService<T extends AbortableRequestExecution> implements Function<Integer, Future<T>>, Closeable {

	protected BrokerClient2 client;
	protected AtomicBoolean abort;
	private ExecutorService executor;
	private WebSocket websocket;

	private Map<Integer, PendingExecution> pending;

	public AbstractExecutionService(BrokerClient2 client, ExecutorService executor) throws IOException {
		this.abort = new AtomicBoolean();
		this.client = client;
		this.pending = Collections.synchronizedMap(new HashMap<>());
		this.executor = executor;
	}

	private class PendingExecution implements Runnable{
		T execution;
		Future<T> future;

		public PendingExecution(T execution) {
			this.execution = execution;
		}
		private void abortLocally() {
			execution.abortLocally();
			if( future != null ) {
				future.cancel(true);
			}
		}
		@Override
		public void run() {
			execution.run();
			onFinished(this);
		}
	}

	/**
	 * Load the previous executions. Implementation might call {@link #pollRequests()} to load
	 * the execution queue from the server. 
	 * Alternatively, local persistence can be used to load requests e.g. from a database.
	 */
	public abstract void loadQueue();

	/**
	 * Start the websocket listener to retrieve live updates about published or closed requests.
	 * To close the websocket connection, call shutdown or close
	 * @throws IOException
	 */
	public void startupWebsocketListener() throws IOException {
		this.websocket = client.openWebsocket(new NotificationListener() {
			
			@Override
			public void onResourceChanged(String resourceName) {
				// nothing happens
			}
			
			@Override
			public void onRequestPublished(int requestId) {
				// check if request already pending
				addRequest(requestId);
			}
			
			@Override
			public void onRequestClosed(int requestId) {
				cancelRequest(requestId, true);
			}
		});
	}
	/**
	 * Abort the executor by shutting down the websocket and aborting all
	 * pending and running executions.
	 */
	@SuppressWarnings("unchecked")
	public void shutdown() {
		websocket.abort();
		this.abort.set(true);
		List<Runnable> aborted = executor.shutdownNow();
		// extract executions from local wrapper PendingExecution
		List<T> list = new ArrayList<>(aborted.size());
		aborted.forEach( (r) -> list.add(((PendingExecution)r).execution) );
		onShutdown(list);
	}

	@Override /* allowed to throw IOException, but we don't */
	public void close() {
		shutdown();
	}

	/**
	 * This method is called during shutdown and can be used to persist the list of unprocessed
	 * executions. During next startup, {@link #loadQueue()} could be used to load and resume the queue. 
	 * @param unprocessedExecutions list of unprocessed executions waiting in the queue.
	 */
	protected abstract void onShutdown(List<T> unprocessedExecutions);
	protected abstract void onStatusUpdate(T execution, RequestStatus status);

	protected abstract T initializeExecution(Integer requestId);

	/**
	 * Callback once the execution is finished. Used to maintain our local queue/cache.
	 * @param execution execution just finished
	 */
	private void onFinished(PendingExecution p) {
		pending.remove(p.execution.getRequestId());
	}

	/**
	 * Add a pending request. This method will be automatically called by the websocket {@code request published} notification.
	 * @param requestId request id to add/execute
	 * @return Future for the request. If the request is already pending, the previous (unfinished) future is returned.
	 */
	public Future<T> addRequest(Integer requestId){
		// if request already pending, return existing future
		PendingExecution p = pending.get(requestId);
		if( p == null ) {
			// request not pending previously
			// this is the normal case
			p = new PendingExecution(initializeExecution(requestId));
			p.execution.setGlobalAbort(abort::get);
			p.execution.setStatusListener((e,s) -> this.onStatusUpdate((T)e, s));
			pending.put(requestId, p);
			this.onStatusUpdate(p.execution, RequestStatus.queued);
			p.future = executor.submit(p, p.execution);
//			alternatively, we could also return CompletableFuture: CompletableFuture.runAsync(p, executor).thenApply((x) -> p.execution);
		}
		return p.future;
	}

	/**
	 * Cancel a pending request. This method will be automatically called by the websocket {@code request closed} notification.
	 * @param requestId request id to cancel
	 * @param expired whether the request expired from the server point of view e.g. closed by the broker. If set to {@code false} the remote status will be updated to rejected.
	 * @return Future for the request if still pending. If the future is not pending anymore (e.g. already finished), then {@code null} is returned.
	 */
	public Future<T> cancelRequest(Integer requestId, boolean expired) {
		PendingExecution p = pending.get(requestId);
		if( p != null ) {
			p.abortLocally();
			if( expired == true ) {
				onStatusUpdate(p.execution, RequestStatus.expired);
			}else {
				// TODO report status update to broker
				onStatusUpdate(p.execution, RequestStatus.rejected);
			}
			return p.future;
		}else {
			return null;
		}
	}

	@Override
	public Future<T> apply(Integer requestId) {
		return addRequest(requestId);
	}


	/**
	 * Poll the server for new requests.
	 */
	public void pollRequests() {
		// TODO implement. ask for requests compare with pending and call cancelRequest or addRequest for changes
	}
}
