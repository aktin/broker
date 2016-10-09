package org.aktin.broker.db;

import java.sql.Connection;
import java.sql.SQLException;

import liquibase.exception.LiquibaseException;

public abstract class AbstractDatabase {

	public abstract Connection getConnection() throws SQLException;

	public void resetDatabase() throws SQLException{
		Connection c = getConnection();
//		try( Connection c = getConnection() ){
			AbstractDatabase.resetDatabase(c);
//		}
	}

	private static final void resetDatabase(Connection dbc)throws SQLException{
		try( LiquibaseWrapper w = new LiquibaseWrapper(dbc) ){
			w.reset();
		} catch (LiquibaseException e) {
			// update failed, make sure the connection is closed
			try{
				dbc.close();
			}catch( SQLException e2 ){
				e.addSuppressed(e2);
			}
			throw new SQLException("Error performing liquibase update", e);
		}		
	}

}
