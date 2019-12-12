package org.aktin.broker.query;

import java.io.PrintStream;
import java.util.logging.Level;

/**
 * Logger interface to handle errors/messages during query processing.
 * @author R.W.Majeed
 *
 */
public interface Logger {

	void log(Level level, String message, String location, Throwable throwable);

	
	public static Logger streamLogger(PrintStream out) {
		return new Logger() {
			@Override
			public void log(Level level, String message, String location, Throwable throwable) {
				if( throwable != null ) {
					throwable.printStackTrace(out);
				}
				out.println(".. by");
				out.println(level.toString()+": "+message);
			}
		};
	}
	public static Logger stderrLogger() {
		return streamLogger(System.err);
	}
}
