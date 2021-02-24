package org.aktin.broker.client.live;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.xml.RequestStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * Abstract request execution.
 * From its main {@link #run()} method, the detailed abstract functions
 * {@link #prepareExecution()}, {@link #doExecution()} and {@link #finishExecution()} are called.
 *
 * @author R.W.Majeed
 *
 */
public abstract class AbstractRequestExecution implements Runnable {
	@Getter
	protected int requestId;

	@Setter
	protected BrokerClient2 client;

	protected long startTimestamp;
	/** when the processing was ended, regardless of successful termination, failure or timeout*/
	protected long exitTimestamp;

	/** Throwable explaining the failure. Can be null e.g. if the process was aborted or had exit code != 0 */
	@Getter
	protected Throwable cause;
	
	/** whether the process completed successfully e.g. not failed or still running */
	private boolean success;

	private BiConsumer<AbstractRequestExecution, RequestStatus> statusListener;
	
	public AbstractRequestExecution(int requestId) {
		this.requestId = requestId;
		this.statusListener = (e,s) -> {}; // default to noop listener
	}

	public boolean isRunning() {
		return exitTimestamp != 0;
	}
	public boolean isFailed() {
		return !isRunning() && !success;
	}
	/**
	 * This method is package-only. Called by the {@link AbstractExecutionService}
	 * @param statusListener status listener
	 */
	void setStatusListener(BiConsumer<AbstractRequestExecution, RequestStatus> statusListener) {
		this.statusListener = statusListener;
	}

	/**
	 * Prepare the execution of the request.
	 * Typically downloads request definition, validates
	 * preconditions, creates temporary files, etc.
	 * @throws IOException IO error. When thrown, {@link #doExecution()} is never called.
	 */
	protected abstract void prepareExecution() throws IOException;
	/**
	 * Perform the execution. This method should not call {@link #reportCompleted()} or {@link #reportFailure(String)}
	 * but may store the result data and failure output.
	 *
	 * If the method blocks for a longer duration, a way of aborting may be implemented by implementations.
	 * 
	 * @throws IOException execution failure due to IO error
	 */
	protected abstract void doExecution() throws IOException;

	/**
	 * Finish the execution, submit the results and status messages. Clean up resources.
	 * This method is always called - even if {@link #prepareExecution()} or {@link #doExecution()} failed.
	 * During finishExecution, call {@link #reportCompleted()} or {@link #reportFailure(String)} 
	 */
	protected abstract void finishExecution();

	/**
	 * Get the mediatype for the result.
	 * This method will be called by the method {@link #reportCompleted()}
	 * after successful execution.
	 * @return internet media type. may include the charset.
	 */
	protected abstract String getResultMediatype();
	/**
	 * Open the input stream containing result data.
	 * This method will be called by the method {@link #reportCompleted()} 
	 * after successful execution.
	 * @return opened input stream. must be closed.
	 * @throws IOException io error
	 */
	protected abstract InputStream getResultData() throws IOException;
	
	protected void reportFailure(String message) {
		try {
			client.postRequestFailed(requestId, message, cause);
			client.deleteMyRequest(requestId);
		} catch (IOException e) {
			if( this.cause != null ) {
				this.cause.addSuppressed(e);
			}
		}

		statusListener.accept(this, RequestStatus.failed);

		// TODO improve logging
		if( cause != null ) {
			cause.printStackTrace();
		}
		System.err.println("Request "+requestId+" failed: "+message);

	}
	protected void reportCompleted() {
		// read stout and report to broker
		try( InputStream in = getResultData() ){
			client.putRequestResult(requestId, getResultMediatype(), in);				
			client.deleteMyRequest(requestId);
		} catch (IOException e) {
			// error during reading the result or reporting the outcome will count as failure
			this.cause = e;
			// logging will be done by reportFailure called below
		}
		if( cause == null ) {
			// post successful completion
			try {
				client.postRequestStatus(requestId, RequestStatus.completed);
			} catch (IOException e) {
				// completed status submission failed. whole process will also count as failed
				this.cause = e;
				// logging will be done by reportFailure called below
			}
		}
		
		if( cause != null ) {
			// process initially completed successfully, but result/status submission failed.
			// try to report failure
			reportFailure("Result/status submission failed");
		}else {
			// successfully completed and result/status submitted
			this.success = true;
			statusListener.accept(this, RequestStatus.completed);
		}
	}

	@Override
	public void run() {
		statusListener.accept(this, RequestStatus.processing);
		try {
			prepareExecution();
		} catch (IOException e) {
			this.cause = e;
			this.exitTimestamp = System.currentTimeMillis();
			finishExecution();
			return; // dont't start the process if the preparation failed
		}

		
		try{
			doExecution();
		}catch( IOException e ) {
			this.cause = e;
		}
		this.exitTimestamp = System.currentTimeMillis();
		finishExecution();
	}

}
