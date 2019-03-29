package org.aktin.broker.query.aggregate.rscript;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

import org.aktin.scripting.r.RScript;


/**
 * SQL export execution.
 * The export is performed in five steps:
 * <ol>
 * 	<li>Prepare SQL</li>
 *  <li>Create tables via {@link #generateTables(Connection)}. 
 *      This step also performs anonymization.</li>
 *  <li>Export tables</li>
 *  <li>Remove tables</li>
 * </ol>
 * @author R.W.Majeed
 *
 */
public class Execution{
	private static final Logger log = Logger.getLogger(Execution.class.getName());

	private RSource script;
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
	}

	public void runRscript() throws IOException {
		RScript r = new RScript(rExecPath);
		r.runRscript(workingDir, workingDir.relativize(mainScript).toString());
		
	}
}
