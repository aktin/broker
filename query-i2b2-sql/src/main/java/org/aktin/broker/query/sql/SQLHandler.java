package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import javax.activation.DataSource;

import org.aktin.broker.query.QueryHandler;

public class SQLHandler implements QueryHandler {
	private SQLHandlerFactory factory;
	private SQLQuery query;
	private Function<String,String> propertyLookup;

	SQLHandler(SQLHandlerFactory factory, SQLQuery query, Function<String,String> propertyLookup){
		this.factory = factory;
		this.query = query;
		this.propertyLookup = propertyLookup;
	}
	@Override
	public Reader getQueryHTML() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResultMediaType() {
		return "application/zip";
	}

	@Override
	public void execute(OutputStream target) throws IOException {
		Execution ex = new Execution(query);
		try {
			ex.prepareStatements(propertyLookup);
		} catch (SubstitutionError e) {
			throw new IOException(e);
		}
		try( Connection dbc = factory.openConnection() ){
			try{
				ex.generateTables(dbc);
				TableExport export = new ZipFileExport(target, factory.getExportCharset());
				ex.exportTables(export);
				export.close();
			}finally{
				ex.removeTables();
			}
		} catch (SQLException e) {
			throw new IOException(e);
		}
	}

	@Override
	public String[] getResultDisplayTypes() {
		return new String[]{};
	}

	@Override
	public InputStream getResultDisplayData(DataSource result, String mediaType) {
		// TODO Auto-generated method stub
		return null;
	}

}
