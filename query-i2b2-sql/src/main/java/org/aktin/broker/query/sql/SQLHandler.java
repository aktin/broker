package org.aktin.broker.query.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import org.aktin.broker.query.Logger;
import org.aktin.broker.query.QueryHandler;
import org.aktin.broker.query.io.MultipartDirectory;
import org.aktin.broker.query.io.MultipartOutputStream;
import org.aktin.broker.query.io.MultipartTableWriter;
import org.aktin.broker.query.io.table.TableExport;

public class SQLHandler implements QueryHandler {
	private SQLHandlerFactory factory;
	private SQLQuery query;
	private Function<String,String> propertyLookup;
	private Logger logger;

	SQLHandler(SQLHandlerFactory factory, SQLQuery query, Function<String,String> propertyLookup){
		this.factory = factory;
		this.query = query;
		this.propertyLookup = propertyLookup;
	}


	@Override
	public void execute(MultipartDirectory input, MultipartOutputStream target) throws IOException {
		Execution ex = new Execution(query, logger);
		try {
			ex.prepareStatements(propertyLookup);
		} catch (SubstitutionError e) {
			throw new IOException(e);
		}
		try( Connection dbc = factory.openConnection() ){
			try( TableExport export = new MultipartTableWriter(target, factory.getExportCharset()) ){
				ex.generateTables(dbc);
				ex.exportTables(export);
			}finally{
				ex.removeTables();
			}
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}


	@Override
	public void setLogger(Logger log) {
		this.logger = log;
	}

}
