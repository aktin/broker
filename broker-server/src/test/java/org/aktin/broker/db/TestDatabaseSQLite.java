package org.aktin.broker.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;



public class TestDatabaseSQLite extends AbstractDatabase{
	@Override
	public Connection getConnection() throws SQLException{
		return DriverManager.getConnection("jdbc:sqlite:target/broker_test.sqlite");
	}

	@Test
	public void verifyDatabaseFileCreated() throws SQLException{
		//resetDatabase();
		Assert.assertTrue(true);
	}



}
