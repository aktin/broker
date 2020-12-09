package org.aktin.broker.admin.standalone;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class HSQLDataSource implements DataSource {
	public HSQLDataSource(String dbpath){
		this.dbpath = dbpath;
	}
	private String dbpath;
	private PrintWriter logWriter;

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return logWriter;
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		this.logWriter = out;
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:hsqldb:file:"+dbpath+";shutdown=false;user=admin;password=secret", "sa", "");
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getConnection();
	}

}
