package org.aktin.broker;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.activation.DataSource;
import javax.ws.rs.core.MediaType;

import org.aktin.broker.db.AbstractDatabase;
import org.aktin.broker.db.AggregatorBackend;
import org.aktin.broker.db.AggregatorImpl;
import org.aktin.broker.db.TestDataSource;
import org.aktin.broker.db.TestDatabaseHSQL;
import org.aktin.broker.xml.ResultInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class TestAggregatorDatabase {

	public TestAggregatorDatabase(AbstractDatabase db){
		this.db = db;
	}
	private AbstractDatabase db;
	private AggregatorBackend backend;
	
	
	@Parameters
	public static Iterable<AbstractDatabase[]> getTestDatabases(){
//		return Arrays.asList(new TestDatabaseHSQL(), new TestDatabaseSQLite());
		List<AbstractDatabase[]> l = new ArrayList<>();
		l.add(new AbstractDatabase[]{new TestDatabaseHSQL()});
		//l.add(new AbstractDatabase[]{new TestDatabaseSQLite()});
		// SQLite not supported yet. Needs different SQL, e.g. without now() function
		return l;
//		return Arrays.asList(new AbstractDatabase[]{new TestDatabaseHSQL()});
	}

	@Before
	public void newDatabase() throws SQLException, IOException{
		backend = new AggregatorImpl(new TestDataSource(db), Paths.get("target/aggregator-data"));
		backend.clearDataDirectory();
	}
	@Test
	public void insertedResultsCanBeRead() throws SQLException, IOException{
		final String testMimeType = "text/vnd.test"+new Random().nextInt();
		final String testContent = "Text "+new Random().nextInt();

		// add result
		backend.addOrReplaceResult(0, 0, MediaType.valueOf(testMimeType), new ByteArrayInputStream(testContent.getBytes()));

		// check result listing
		List<ResultInfo> list = backend.listResults(0);
		assertEquals(1, list.size());
		assertEquals(testMimeType, list.get(0).type);

		// read back result
		DataSource data = backend.getResult(0, 0);

		// verify data
		assertNotNull(data);
		assertEquals(testMimeType, data.getContentType());
		String content;
		try( BufferedReader r = new BufferedReader(new InputStreamReader(data.getInputStream())) ){
			content = r.readLine();
		}
		assertEquals(testContent, content);
	}
}
