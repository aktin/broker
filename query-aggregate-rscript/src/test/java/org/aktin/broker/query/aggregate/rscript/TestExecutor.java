package org.aktin.broker.query.aggregate.rscript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXB;

import org.aktin.scripting.r.TestRScript;
import org.junit.Test;

public class TestExecutor {

	public static RSource getRScript() throws IOException{
		RSource q;
		try( InputStream in = TestExecutor.class. getResourceAsStream("/query-rscript.xml") ){
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

	private static final String testDataDir = "/data/";
	private static final String[] testData1 = {"temp_encounters.txt"};
	private void copyInputFiles1(Path dest) throws IOException {
		// copy test data files
		for( int i=0; i<testData1.length; i++ ) {
			String name = testData1[i];
			try( InputStream in = TestExecutor.class.getResourceAsStream(testDataDir+name) ){
				Files.copy(in, dest.resolve(name));
			}
		}
	}
	private void deleteInputFiles1(Path base) throws IOException {
		for( int i=0; i<testData1.length; i++ ) {
			String name = testData1[i];
			Files.delete(base.resolve(name));
		}		
	}
	@Test
	public void executeRScript() throws IOException {
		RSource rs = getRScript();
		
		Execution exec = new Execution(rs);
		Path testDir = Files.createTempDirectory("aggregate-r");
		copyInputFiles1(testDir);

		exec.setRScriptExecutable(TestRScript.findR());
		exec.setWorkingDir(testDir);
		exec.createFileResources();


		exec.runRscript();

		exec.removeFileResources();
		deleteInputFiles1(testDir);
		exec.removeResultFiles();

		Files.delete(testDir);
	}
}
