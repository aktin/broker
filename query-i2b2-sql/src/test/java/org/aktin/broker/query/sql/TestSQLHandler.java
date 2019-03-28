package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.aktin.broker.query.io.ZipArchiveWriter;
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
		try( OutputStream out = Files.newOutputStream(temp);
				ZipArchiveWriter zip = new ZipArchiveWriter(out, StandardCharsets.UTF_8)){
			h.execute(zip);
		}
	}
}
