package org.aktin.broker.query.aggregate.rscript;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aktin.broker.query.io.MultipartDirectoryWriter;
import org.aktin.broker.query.io.MultipartOutputStream;
import org.aktin.scripting.r.AbnormalTerminationException;
import org.aktin.scripting.r.RScript;


/**
 * R script execution which can be
 * applied after data was extracted.
 * @author R.W.Majeed
 *
 */
public class Execution{
	private static final Logger log = Logger.getLogger(Execution.class.getName());

	private RSource script;
	/** timeout milliseconds for the script */
	private Integer timeoutMillis;
	/** working directory, where input data resides and the Rscript command is executed */
	private Path workingDir;

	/** path to the main script to be executed. will be set by {@link #createFileResources()} */
	private Path mainScript;

	/** Path to the Rscript executable */
	private Path rExecPath;
	
	public Execution(RSource script){
		this.script = script;
	}
	public void setWorkingDir(Path dir) {
		this.workingDir = dir;
	}
	public void setRScriptExecutable(Path path) {
		rExecPath = path;
	}

	/**
	 * Resolve path withing the working directory.
	 * Also makes sure that the names do not go outside (e.g. up) from the working dir
	 * @param name file name or sub path
	 * @return resolved path
	 */
	private Path resolvePath(String name) {
		// TODO verify that the resulting path is still in the workingDir
		return this.workingDir.resolve(name);
	}

	private void parseTimeout() throws IOException {
		if( script.source.timeout != null && script.source.timeout.length() > 0 ) {
			Matcher m = Pattern.compile("([0-9]+)(m?s)").matcher(script.source.timeout);
			if( !m.matches() ) {
				throw new IOException("Script timeout value not parsable: "+script.source.timeout);
			}
			int value = Integer.valueOf(m.group(1));
			switch( m.group(2) ) {
			case "ms":
				timeoutMillis = value;
				break;
			case "s":
				timeoutMillis = value*1000;
				break;
			default:
				// should never happen, since the regex already verifies only s or ms is used.
				throw new IOException("Unsupported timeout unit: "+m.group(2));
			}
		}else {
			// no timeout
			timeoutMillis = null;
		}		
	}
	/**
	 * Create the  main script file and additional resources
	 * in the working directory.
	 * @throws IOException IO error
	 */
	public void createFileResources() throws IOException{
		// create main script
		mainScript = Files.createTempFile(this.workingDir, "main", ".R");
		log.info("Created source file for R script query postprocessing: "+mainScript);
		try( BufferedWriter w = Files.newBufferedWriter(mainScript, StandardOpenOption.TRUNCATE_EXISTING) ){
			w.write(script.source.value);
		}
		parseTimeout();
		for( Resource r : script.resource ) {
			try( BufferedWriter w = Files.newBufferedWriter(resolvePath(r.file), StandardOpenOption.CREATE_NEW) ){
				w.write(r.value);
			}			
		}
	}
	public void removeFileResources() throws IOException{
		// delete main script if previously created
		if( mainScript != null ) {
			Files.delete(mainScript);
		}
		for( Resource r : script.resource ) {
			Files.delete(resolvePath(r.file));	
		}
	}
	public void removeResultFiles() throws IOException{
		// delete main script if previously created
		for( Result r : script.result ) {
			Files.delete(resolvePath(r.file));
		}
		if( script.resultList != null ) {
			for( Result r : script.resultList.getFileEntries(this::resolvePath) ) {
				Files.delete(resolvePath(r.file));
			}
		}
	}

	public void runRscript() throws IOException{
		RScript r = new RScript(rExecPath);
		try {
			r.runRscript(workingDir, workingDir.relativize(mainScript).toString(), timeoutMillis);
		} catch (TimeoutException e) {
			throw new IOException("R execution timeout expired", e);
		} catch (AbnormalTerminationException e) {
			// log full error message
			log.warning("R execution failed with exit code "+e.getExitCode());
			log.warning("R stdout: "+e.getErrorOutput());
			throw new IOException("R execution failed",e);
		}
	}

	private void copyFilesToTargetStream(List<Result> results, MultipartOutputStream target) throws IOException {
		for( Result r : results ) {
			Path file = resolvePath(r.file);
			try( OutputStream out = target.writePart(r.type, r.file) ){
				Files.copy(file, out);
			}
		}
	}
	private void copyFileEntriesOnly(List<Result> results, MultipartDirectoryWriter w) throws FileNotFoundException {
		// result entries
		for( Result r : results ) {
			Path file = resolvePath(r.file);
			if( Files.exists(file) ) {
				// file exists, write file entry
				w.addFileEntry(r.file, r.type);
			}else if( r.getRequired() ) {
				// file does not exist but is required
				throw new FileNotFoundException("Required result file does not exist: "+file);
			}
		}		
	}
	public void moveResultFiles(MultipartOutputStream target) throws IOException{
		// if the target directory is the same as the working directory,
		// then we don't need copy the data
		if( target instanceof MultipartDirectoryWriter 
				&& this.workingDir.equals(((MultipartDirectoryWriter)target).getBasePath()) ) {
			// files already there, just create the entries
			MultipartDirectoryWriter w = (MultipartDirectoryWriter)target;
			// result list
			copyFileEntriesOnly(script.result, w);
			if( script.resultList != null ) {
				copyFileEntriesOnly(script.resultList.getFileEntries(this::resolvePath), w);
			}
			// TODO read file script.resultList and add contained references
		}else {
			// move file data to the target stream
			copyFilesToTargetStream(script.result, target);
			if( script.resultList != null ) {
				copyFilesToTargetStream(script.resultList.getFileEntries(this::resolvePath), target);
			}
			// all result files copied, now delete the local data
			removeResultFiles();
		}
	}
}
