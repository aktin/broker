package org.aktin.broker.query.sql;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

public class TestSQLHandler {

	public static DataSource getLocalI2b2DataSource(){
		PGSimpleDataSource ds = new PGSimpleDataSource();
		ds.setServerName("localhost");
		ds.setPortNumber(15432);
		ds.setDatabaseName("i2b2");
		ds.setUser("i2b2crcdata");
		return ds;
	}
	public static void main(String[] args) throws IOException{
		SQLHandlerFactory f = new SQLHandlerFactory(getLocalI2b2DataSource());
		SQLHandler h = f.wrap(TestExecutor.getTestQuery(), TestExecutor.getTestLookup()::get);
		// create ZIP file
		Path temp = Files.createTempFile("queries", ".zip");
		System.out.println("Writing to "+temp.toString());
		// write ZIP file
		h.execute(temp);
	}
}
