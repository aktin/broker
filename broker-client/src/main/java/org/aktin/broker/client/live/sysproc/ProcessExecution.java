package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import org.aktin.broker.client.live.AbortableRequestExecution;
import org.aktin.broker.client2.validator.RequestValidator;
import org.aktin.broker.client2.validator.ValidationError;
import org.aktin.broker.xml.RequestStatus;

import lombok.Setter;

/**
 * Single process execution.
 * To abort a running execution, make sure the isAborted {@link BooleanSupplier} returns false 
 * and interrupt the running thread.
 *
 * In case of an execution timeout, {@link #getCause()} will contain an instance of {@link TimeoutException}.
 * In case of abort during execution, {@link #getCause()} will contain an instance of {@link CancellationException}
 * @author R.W.Majeed
 *
 */
public class ProcessExecution extends AbortableRequestExecution{
	private ProcessExecutionConfig config;
	
	/**
	 * Whether or not to keep temporary files. If set to {@code false} the stdin and stdou
	 * of the process are not deleted after completion.
	 * To delete these files manually, you can call {@link #cleanTempFiles()}
	 */
	@Setter
	private boolean keepTempFiles;

	private ProcessBuilder pb;
	private Process proc;
	private Path stdin;
	private Path stdout;

	public ProcessExecution(int requestId, ProcessExecutionConfig config) {
		super(requestId);
		this.config = config;
	}

	private void validateRequest(int requestId) throws IOException, ValidationError {
		RequestValidator validator;
		try{
			validator = config.getRequestValidator().getValidator(config.getRequestMediatype());
		}catch( IllegalArgumentException e ) {
			throw new IOException("Validator not available for media type: "+config.getRequestMediatype(), e);
		}
		try( InputStream in = Files.newInputStream(stdin) ){
			validator.validate(in);
		}
	}

	@Override
	protected void prepareExecution() throws IOException {

		stdin = Files.createTempFile("stdin", null);
//		System.out.println("Loading request definition "+requestId+" to temp file "+stdin.toString());
		// retrieve request
		Path resp = client.getMyRequestDefinition(requestId, config.getRequestMediatype(), stdin, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		if( resp == null ) {
			throw new IOException("Request not found");
		}
		client.postRequestStatus(requestId, RequestStatus.retrieved);
		
		// allow custom validation if configured
		if( config.getRequestValidator() != null ) {
			try {
				validateRequest(requestId);
			} catch (ValidationError e) {
				throw new IOException("Request validation failed", e);
				// alternatively, we could reject the request via explicit rejected status.. in the end no big difference
			}
		}
		
		this.pb = new ProcessBuilder(config.getCommand());
		// add env? pb.environment().put(key, value)
		// add env? from .env properties file
		
		pb.redirectInput(Redirect.from(stdin.toFile()));
		stdout = Files.createTempFile("stdout", null);
		pb.redirectOutput(Redirect.to(stdout.toFile()));
		pb.redirectError(Redirect.DISCARD); // TODO later write to file or read nonblocking for progress
		
		// log request contents
		if( config.getProcessLogDir() != null ) {
			// time stamp and request-id are derivable from filename and last-changed date
			// log request contents
			Files.copy(stdin, config.getProcessLogDir().resolve(getRequestId()+"-request"));
			// TODO also log metadata e.g. process command, metadata, etc. environment etc.
		}
	}
	
	@Override
	protected void doExecution() throws IOException{
		this.proc = pb.start();
		// notify broker that we are now processing the query
		long readProgressIntervalMillis = 10*1000;
		long maxWait = Math.min(readProgressIntervalMillis, config.getProcessTimeoutMillis());
		client.postRequestStatus(requestId, RequestStatus.processing);
		try( InputStream stderr = proc.getErrorStream() ){
			while( !isAborted() && proc.isAlive() && (System.currentTimeMillis()-getStartTimestamp()) < config.getProcessTimeoutMillis() ) {
				// later, check here for progress in stderr. 
				// waitFor(10s) will continuously allow the progress to be read
				try {
					proc.waitFor(maxWait, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					// interruptions may occur regularly. e.g. by adding to the queue
				}
			}
			// later, check/read remaining progress from stderr
		}		
	}

	/* To read execution progress from stderr nonblocking, 
	 * use the following method:
	 * 
	 *			
	 *			CharsetDecoder dec = StandardCharsets.UTF_8.newDecoder();
				CharBuffer buffer = CharBuffer.allocate(1024*10);
				StringBuilder lines = new StringBuilder();
				// each timeout of process.waitFor(10sec) try to read available data from stderr
				int avail = stderr.available();
				while( avail > 0 ) {
					byte[] buf = new byte[avail];
					int r = stderr.read(buf);
					dec.decode(ByteBuffer.wrap(buf, 0, r), buffer, false);
					buffer.flip();
					// copy decoded bytes to string builder
					lines.append(buffer.toString());
					buffer.position(buffer.limit());
					buffer.compact();
				}
				int newline = lines.lastIndexOf("\n");
				if( newline != -1 ) {
					processStderrProgress(lines.substring(0, newline+1));
					lines.delete(0, newline+1);
				}
				
	 */

	

	public void cleanTempFiles() {
		// clear temp files
		try {
			Files.delete(stdin);
		} catch (IOException e) {
			if( cause != null ) {
				cause.addSuppressed(e);
			}
			// TODO log warning
		}
		try {
			Files.delete(stdout);
		} catch (IOException e) {
			if( cause != null ) {
				cause.addSuppressed(e);
			}
			// TODO log warning
		}
	}


	@Override
	protected void finishExecution() {
		if( this.cause != null ) {
			// IO error during execution/preparation
			if( proc != null && proc.isAlive() ) {
				proc.destroy();
			}
			reportFailure("IO error during execution");
		}
		else if( isAborted() == true ) {
			// controlled exit/abort
			if( proc.isAlive() ) {
				proc.destroy();
			}
			this.cause = new CancellationException("Process termination by controlled abort");
			reportFailure(this.cause.getMessage());
		}
		// check if timeout occurred
		else if( proc.isAlive() ) {
			// we have a timeout, kill process
			proc.destroy();
			this.cause = new TimeoutException("External process timeout");
			reportFailure(this.cause.getMessage());

		}else if( proc.exitValue() == 0 ) {
			// normal termination
			reportCompleted(); // will upload the result
			

		}else {
			// abnormal termination
			reportFailure("External process failed with exit code "+proc.exitValue());

		}
		if( !keepTempFiles ) {
			cleanTempFiles();
		}
	}


	@Override
	protected String getResultMediatype() {
		return config.getResultMediatype();
	}

	@Override
	protected InputStream getResultData() throws IOException{
		return Files.newInputStream(getResultPath());
	}
	protected Path getResultPath() {
		return stdout;
	}

	@Override
	protected void reportFailure(String message) {
		super.reportFailure(message);
		// TODO log failure
	}

	@Override
	protected void reportCompleted() {
		super.reportCompleted();
		// TODO log result
	}
}
