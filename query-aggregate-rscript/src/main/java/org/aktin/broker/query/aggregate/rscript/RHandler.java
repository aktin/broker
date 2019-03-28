package org.aktin.broker.query.aggregate.rscript;

import java.io.IOException;
import java.util.function.Function;

import org.aktin.broker.query.QueryHandler;
import org.aktin.broker.query.io.MultipartDirectory;
import org.aktin.broker.query.io.MultipartOutputStream;

public class RHandler implements QueryHandler {
	private RHandlerFactory factory;
	private RSource script;
	private Function<String,String> propertyLookup;

	RHandler(RHandlerFactory factory, RSource query, Function<String,String> propertyLookup){
		this.factory = factory;
		this.script = query;
		this.propertyLookup = propertyLookup;
	}

	@Override
	public void execute(MultipartDirectory input, MultipartOutputStream target) throws IOException {
		Execution ex = new Execution(script);
	}
}
