package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXB;

public class TestExecutor {

	public static SQLQuery getTestQuery() throws IOException{
		SQLQuery q;
		try( InputStream in = TestExecutor.class. getResourceAsStream("/query-sql.xml") ){
			q = JAXB.unmarshal(in,SQLQuery.class);
		}
		return q;
	}

	public static Map<String,String> getTestLookup(){
		Map<String, String> m = new HashMap<>();
		m.put("data.start", "2016-01-01");
		m.put("data.end", "2017-12-31");
		return m;
	}
	public static void main(String[] args) throws SQLException, SubstitutionError, IOException{
		Execution x = new Execution(getTestQuery());
		x.prepareStatements(getTestLookup()::get);
		try( Connection c = TestSQLHandler.getLocalI2b2DataSource().getConnection() ){
			x.generateTables(c);
			x.exportTables(new ConsoleTableExport());
			x.removeTables();
		}
	}
}
