package org.aktin.broker.client.live.sysproc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.aktin.broker.client.live.AbortableRequestExecution;
import org.aktin.broker.xml.RequestStatus;

/**
 * Single process execution.
 * To abort a running execution, make sure the isAborted {@link BooleanSupplier} returns false 
 * and interrupt the running thread.
 * 
 * @author R.W.Majeed
 *
 */
public class ProcessExecution extends AbortableRequestExecution{
	private ProcessExecutionConfig config;
	
	private boolean keepTempFiles;

	private ProcessBuilder pb;
	private Process proc;
	private Path stdin;
	private Path stdout;

	public ProcessExecution(int requestId, ProcessExecutionConfig config) {
		super(requestId);
		this.config = config;
	}

	@Override
	protected void prepareExecution() throws IOException {
		// remember timestamp 
		this.startTimestamp = System.currentTimeMillis();

		stdin = Files.createTempFile("stdin", null);
		System.out.println("Loading request definition "+requestId+" to temp file "+stdin.toString());
		// retrieve request
		client.getMyRequestDefinition(requestId, config.getRequestMediatype(), BodyHandlers.ofFile(stdin, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE));

		this.pb = new ProcessBuilder(config.getCommand());
		// add env pb.environment().put(key, value)
		// add env from .env properties file
		
		pb.redirectInput(Redirect.from(stdin.toFile()));
		stdout = Files.createTempFile("stdout", null);
		pb.redirectOutput(Redirect.to(stdout.toFile()));
		pb.redirectError(Redirect.DISCARD); // TODO later write to file or read nonblocking for progress
	}
	
	@Override
	protected void doExecution() throws IOException{
		this.proc = pb.start();
		// notify broker that we are now processing the query
		client.postRequestStatus(requestId, RequestStatus.processing);
		try( InputStream stderr = proc.getErrorStream() ){
			while( !isAborted() && proc.isAlive() && (System.currentTimeMillis()-this.startTimestamp) < config.getProcessTimeoutMillis() ) {
				// later, check here for progress in stderr. 
				// waitFor(10s) will continuously allow the progress to be read
				try {
					proc.waitFor(60, TimeUnit.SECONDS);
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

	

	private void cleanTempFiles() {
		if( !keepTempFiles ) {
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
			reportFailure("Process termination by controlled abort");
		}
		// check if timeout occurred
		else if( proc.isAlive() ) {
			// we have a timeout, kill process
			proc.destroy();
			reportFailure("External process timeout");

		}else if( proc.exitValue() == 0 ) {
			// normal termination
			reportCompleted();

		}else {
			// abnormal termination
			reportFailure("External process failed with exit code "+proc.exitValue());

		}
		cleanTempFiles();		
	}

	@Override
	protected String getResultMediatype() {
		return config.getRequestMediatype();
	}

	@Override
	protected InputStream getResultData() throws IOException{
		return Files.newInputStream(stdout);
	}
	
}
