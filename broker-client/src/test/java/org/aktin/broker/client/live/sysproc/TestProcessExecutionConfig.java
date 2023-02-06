package org.aktin.broker.client.live.sysproc;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestProcessExecutionConfig {

	@Test
	public void verifyParameterReplacement() {
		Map<String, String> map = new HashMap<>();
		map.put("bla", "xor");
		String input = "foo${bla}bar${blub}";
		String output = ProcessExecutionConfig.lookupPlaceholders(input, map::get);
		Assertions.assertEquals("fooxorbar", output);
	}
	
	
}
