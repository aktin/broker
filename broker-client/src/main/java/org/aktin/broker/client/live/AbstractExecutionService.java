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
import org.aktin.broker.client2.ClientNotificationListener;
import org.aktin.broker.xml.RequestInfo;
import org.aktin.broker.xml.RequestStatus;

import lombok.Getter;

/**
 * Abstract execution service running request executions with the provided executor.
 * Once the service is running, it can be stopped via {@link #shutdown()}. To wait for
 * the service to be finished, call {@link #wait()} on this instance. If woken up from {@link #wait()},
 * check {@link #isAborted()} to make sure a shutdown was initiated (and not any other interruption).
 * 
 * @author R.W.Majeed
 *
 * @param <T> request execution implementation
 */
public abstract class AbstractExecutionService<T extends AbortableRequestExecution> implements Function<Integer, Future<T>>, Closeable {

	@Getter
	protected BrokerClient2 client;
	protected AtomicBoolean abort;
	private ExecutorService executor;

	private Map<Integer, PendingExecution> pending;

	public AbstractExecutionService(BrokerClient2 client, ExecutorService executor){
		this.abort = new AtomicBoolean();
		this.client = client;
		this.pending = Collections.synchronizedMap(new HashMap<>());
		this.executor = executor;
		this.client.addListener(new ClientNotificationListener() {
			
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

			@Override
			public void onWebsocketClosed(int statusCode) {
				AbstractExecutionService.this.onWebsocketClosed(statusCode);
			}
		});
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

	public boolean isAborted() {
		return abort.get();
	}
	/**
	 * Load the previous executions. Implementation might call {@link #pollRequests()} to load
	 * the execution queue from the server. 
	 * Alternatively, local persistence can be used to load requests e.g. from a database.
	 * @throws IOException IO error during loading of queue
	 */
	public abstract void loadQueue() throws IOException;

	public boolean isWebsocketClosed() {
		return client.getWebsocket() == null || client.getWebsocket().isInputClosed();
	}
	/**
	 * Start the websocket listener to retrieve live updates about published or closed requests.
	 * To close the websocket connection, call shutdown or close
	 * @throws IOException
	 */
	public void startupWebsocketListener() throws IOException {
		if( client.getWebsocket() != null ) {
			// close previous websocket
			client.closeWebsocket();
		}
		client.connectWebsocket();
	}
	/**
	 * Abort the executor by shutting down the websocket and aborting all
	 * pending and running executions.
	 * The method will also call {@link #notifyAll()} to notify threads waiting
	 * for a successful shutdown. If woken up, check {@link #isAborted()} whether
	 * a shutdown is in progress.
	 */
	@SuppressWarnings("unchecked")
	public List<T> shutdown() {
		client.closeWebsocket();
		this.abort.set(true);
		List<Runnable> aborted = executor.shutdownNow();
		// extract executions from local wrapper PendingExecution
		List<T> list = new ArrayList<>(aborted.size());
		aborted.forEach( (r) -> list.add(((PendingExecution)r).execution) );
		onShutdown(list);

		// notify threads waiting on this object
		synchronized( this ) {
			this.notifyAll();
		}

		return list;
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
	protected abstract void onWebsocketClosed(int status);

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
	@SuppressWarnings("unchecked")
	public Future<T> addRequest(Integer requestId){
		// if request already pending, return existing future
		PendingExecution p = pending.get(requestId);
		if( p == null ) {
			// request not pending previously
			// this is the normal case
			p = new PendingExecution(initializeExecution(requestId));
			p.execution.setClient(client);
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
	 * @throws IOException IO error during polling
	 */
	public void pollRequests() throws IOException {
		List<RequestInfo> list = client.listMyRequests();
		for( RequestInfo request : list ) {
			if( request.closed != null ) {
				// closed request. see if it is in the queue
				PendingExecution p = pending.get(request.getId());
				if( p != null ) {
					p.abortLocally();
				}else {
					// we don't have the execution in our queue
					;// nothing to do
				}
				client.deleteMyRequest(request.getId());
			}else{
				// request still open, check if it is already queued
				PendingExecution p = pending.get(request.getId());
				if( p == null ) {
					// add request to queue
					addRequest(request.getId());
				}else {
					// request already queued
					;// nothing to do
				}
				
			}
		}
		// TODO there might be requests queued, which are no longer on the server. these should be canceled too
	}
}
