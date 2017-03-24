package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import javax.activation.DataSource;

import org.aktin.broker.query.QueryHandler;
import org.aktin.broker.query.util.ReadOnlyPathDataSource;

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
	public DataSource execute(Path target) throws IOException {
		Execution ex = new Execution(query);
		try {
			ex.prepareStatements(propertyLookup);
		} catch (SubstitutionError e) {
			throw new IOException(e);
		}
		try( Connection dbc = factory.openConnection() ){
			try{
				ex.generateTables(dbc);
				try( OutputStream out = Files.newOutputStream(target);
					 TableExport export = new ZipFileExport(out, factory.getExportCharset()) ){
					ex.exportTables(export);
				}
			}finally{
				ex.removeTables();
			}
		} catch (SQLException e) {
			throw new IOException(e);
		}
		return new ReadOnlyPathDataSource(target, getResultMediaType());
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
