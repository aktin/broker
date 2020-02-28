package org.aktin.broker.query.aggregate.rscript;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

import org.aktin.broker.query.Logger;
import org.aktin.broker.query.QueryHandler;
import org.aktin.broker.query.io.MultipartDirectory;
import org.aktin.broker.query.io.MultipartOutputStream;

public class RHandler implements QueryHandler {
	private RHandlerFactory factory;
	private RSource script;
	private Logger log;
//	private Function<String,String> propertyLookup;

	RHandler(RHandlerFactory factory, RSource query, Function<String,String> propertyLookup){
		this.factory = factory;
		this.script = query;
//		this.propertyLookup = propertyLookup;
	}

	@Override
	public void execute(MultipartDirectory input, MultipartOutputStream target) throws IOException {
		Execution ex = new Execution(script);
		Objects.requireNonNull(log,"Logger required for execution");
		ex.setRScriptExecutable(factory.getRExecutablePath());
		ex.setWorkingDir(input.getBasePath());
		ex.createFileResources();
		ex.runRscript();
		ex.removeFileResources();
		ex.moveResultFiles(target);
	}

	@Override
	public void setLogger(Logger log) {
		this.log = log;
	}
}
