package org.aktin.broker.query.sql;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXB;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestJAXB {

	@Test
	public void unmarshallTestResource() throws IOException, SubstitutionError{
		SQLQuery query;
		try( InputStream in = getClass().getResourceAsStream("/query-sql.xml") ){
			query = JAXB.unmarshal(in, SQLQuery.class);
		}
		assertNotNull(query);
		assertEquals(1, query.source.size());
		assertEquals(2, query.anonymize.size());
		assertEquals(3, query.export.size());
		
		Source s = query.source.get(0);
		List<String> l = new ArrayList<>();
		s.splitStatements(null, l::add);
		assertEquals(3, l.size());
		for( String command : l ){
			System.out.println("Command: "+command);
		}
	
		assertEquals("temp_patients", query.anonymize.get(0).key.table);
		assertEquals("id", query.anonymize.get(0).key.column);
	}
}
