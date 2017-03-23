package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXB;

public class TestExecutor {

	public static void main(String[] args) throws SQLException, SubstitutionError, IOException{
		SQLQuery q;
		try( InputStream in = TestExecutor.class. getResourceAsStream("/query-sql.xml") ){
			q = JAXB.unmarshal(in,SQLQuery.class);
		}
		Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:15432/i2b2", "i2b2crcdata", "");
		Execution x = new Execution(c, q);
		Map<String, String> m = new HashMap<>();
		m.put("data.start", "2016-01-01");
		m.put("data.end", "2017-12-31");
		x.prepareStatements(m::get);
		x.run();
	}
}
