package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.zip.ZipInputStream;

import javax.activation.DataSource;

import org.aktin.broker.query.QueryHandler;
import org.aktin.broker.query.io.MultipartOutputStream;
import org.aktin.broker.query.io.MultipartTableWriter;
import org.aktin.broker.query.io.TableExport;

public class SQLHandler implements QueryHandler {
	private SQLHandlerFactory factory;
	private SQLQuery query;
	private Function<String,String> propertyLookup;
	private Path resultDisplay;

	SQLHandler(SQLHandlerFactory factory, SQLQuery query, Function<String,String> propertyLookup){
		this.factory = factory;
		this.query = query;
		this.propertyLookup = propertyLookup;
	}
	@Override
	public DataSource getQueryVisualisation(String mediaType) throws IOException{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getResultMediaType() {
		return "application/zip";
	}

	@Override
	public void execute(MultipartOutputStream target) throws IOException {
		Execution ex = new Execution(query);
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
	public String[] getResultDisplayTypes() {
		return new String[]{"text/plain"};
	}

	@Override
	public DataSource getResultVisualisation(DataSource result, String mediaType) throws IOException {
		if( resultDisplay == null ){
			// TODO create temporary file and store in resultDisplay
			try( InputStream in = result.getInputStream();
				ZipInputStream z = new ZipInputStream(result.getInputStream()) )
			{
				
			}
		}
		// already created, return existing data

		return null;
	}

}
