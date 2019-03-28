package org.aktin.broker.query.aggregate.rscript;


import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXB;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestJAXB {

	@Test
	public void unmarshallTestResource() throws IOException{
		RSource query;
		try( InputStream in = getClass().getResourceAsStream("/query-rscript.xml") ){
			query = JAXB.unmarshal(in, RSource.class);
		}
		assertNotNull(query);
		assertEquals(1, query.resource.size());
		assertEquals(2, query.result.size());
		assertEquals("text/plain", query.resource.get(0).type);
		assertEquals("module1.R", query.resource.get(0).file);

		assertEquals("text/tab-separated-values", query.result.get(0).type);
		assertEquals("counts.txt", query.result.get(0).file);

		assertNotNull(query.source);
		assertNotNull(query.source.value);
		assertEquals("application/r-script", query.source.type);
		assertEquals("30s", query.source.timeout);
		
		
	}
}
