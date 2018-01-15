/**
 * 
 */
package org.aktin.broker.db;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.aktin.broker.db.LiquibaseWrapper;
import org.junit.Assert;
import org.junit.Test;

import liquibase.exception.LiquibaseException;

/**
 * Verifies that the liquibase changelog file
 * can be loaded.
 * <p>
 * For external tests against the database,
 * a temporary database connection can be obtained
 * via {@link #createTestConnection()}
 *  
 * @author R.W.Majeed
 *
 */
public class TestDatabaseHSQL extends AbstractDatabase {

	/**
	 * Create in-memory database containing the AKTIN tables
	 * and default data.
	 * <p>
	 * The database will be closed when {@link Connection#close()} 
	 * is called.
	 * 
	 * @return connection to in-memory database 
	 * @throws SQLException 
	 */
//	public static final Connection createTestConnection() throws SQLException{
//		TestDatabaseHSQL test = new TestDatabaseHSQL();
//		Connection dbc = test.getConnection();
//		initializeDatabase(dbc);
//		return dbc;
//	}
//	
	public static final void initializeDatabase(Connection dbc) throws SQLException{
		try( LiquibaseWrapper w = new LiquibaseWrapper(dbc) ){
			w.update();
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

	public Connection getConnection() throws SQLException{
		return DriverManager.getConnection("jdbc:hsqldb:mem:broker_test", "sa", "");
	}
	
	@Test
	public void changelogResourceIsReadable() throws IOException{
		try( InputStream in = this.getClass().getResourceAsStream(LiquibaseWrapper.CHANGELOG_RESOURCE) ){
			Assert.assertNotNull(in);
		}
		
		URL url = this.getClass().getResource(LiquibaseWrapper.CHANGELOG_RESOURCE);
		System.out.println(url.toExternalForm());
		
		Assert.assertNotNull(getClass().getClassLoader().getResources(LiquibaseWrapper.CHANGELOG_RESOURCE));
		
	}
	
	@Test
	public void loadChangelogToLiquibase() throws SQLException, LiquibaseException, IOException {
		try( LiquibaseWrapper lw = new LiquibaseWrapper(getConnection()) )
		{
			lw.update();
		}
	}
	

}
