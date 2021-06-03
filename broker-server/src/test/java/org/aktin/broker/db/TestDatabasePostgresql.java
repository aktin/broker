package org.aktin.broker.db;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import org.aktin.broker.auth.Principal;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.AuthRole;
import org.junit.Assert;
import org.junit.Test;



public class TestDatabasePostgresql extends AbstractDatabase{
	private String jdbcUrl;

	@Override
	public Connection getConnection() throws SQLException{
		return DriverManager.getConnection("jdbc:sqlite:target/broker_test.sqlite");
	}

	@Test
	public void verifyDatabaseFileCreated() throws SQLException{
		//resetDatabase();
		Assert.assertTrue(true);
	}
	
	public static void main(String [] args) throws IOException, SQLException {
		TestDatabasePostgresql test = new TestDatabasePostgresql();
		test.jdbcUrl = "jdbc:postgresql://localhost/postgres?user=postgres&password=mysecretpassword";
		DataSource ds;
		try {
			Class<? extends DataSource> clazz = Class.forName("org.postgresql.ds.PGSimpleDataSource").asSubclass(DataSource.class);
			ds = clazz.getConstructor().newInstance();
			clazz.getMethod("setURL", String.class).invoke(ds, test.jdbcUrl);
		} catch ( Exception e) {
			throw new RuntimeException("Unable to initialize PostgreSQL DataSource", e);
		}
		BrokerImpl impl = new BrokerImpl(ds, Paths.get("target/"));
//		try( Connection c = ds.getConnection() ){
//			AbstractDatabase.resetDatabase(c);
//		}
	
		Principal p = impl.accessPrincipal(new AuthInfoImpl("123", "CN=bla", AuthRole.ALL_NODE));
		System.out.println(p.getNodeId());
		System.out.println(impl.getAllNodes().size());
	}



}
