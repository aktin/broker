package org.aktin.broker.query.sql;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestSource {

	@Test
	public void verifyPropertySubstitution() throws SubstitutionError{
		
		StringBuilder str = new StringBuilder("A${x1}B${x2}${x3}C");
		Map<String, String> lookup = new HashMap<>();
		lookup.put("x1", "y");
		lookup.put("x2", "z");
		lookup.put("x3", "");
		Source source = new Source();
		
		source.replacePlaceholders(str, lookup::get);
		assertEquals("AyBzC", str.toString());
	}

	@Test
	public void expectSubstitutionErrors(){
		Source source = new Source();
		Map<String, String> lookup = new HashMap<>();
		
		try {
			source.replacePlaceholders(new StringBuilder("A${x1"), lookup::get);
			fail("Unterminated placeholder error expected");
		} catch (SubstitutionError e) {
			// ok
		}
		try {
			source.replacePlaceholders(new StringBuilder("A${x1}"), lookup::get);
			fail("Placeholder not found error expected");
		} catch (SubstitutionError e) {
			// ok
		}
	}

}
