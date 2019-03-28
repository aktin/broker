package org.aktin.broker.query.aggregate.rscript;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXB;

import org.aktin.scripting.r.RScript;
import org.junit.Test;

public class TestExecutor {

	public static RSource getRScript() throws IOException{
		RSource q;
		try( InputStream in = TestExecutor.class. getResourceAsStream("/query-r.xml") ){
			q = JAXB.unmarshal(in,RSource.class);
		}
		return q;
	}

	public static Map<String,String> getTestLookup(){
		Map<String, String> m = new HashMap<>();
		m.put("data.start", "2016-01-01");
		m.put("data.end", "2017-12-31");
		return m;
	}

}
