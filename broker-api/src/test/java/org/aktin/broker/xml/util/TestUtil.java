package org.aktin.broker.xml.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.Assert;
import org.junit.Test;

public class TestUtil {
	/**
	 * Make sure valid HTTP dates are produced (e.g. for Date header).
	 * According to RFC, these must always end in GMT
	 */
	@Test
	public void verifyValidHttpDateStrings(){
		String str = DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.now().atOffset(ZoneOffset.UTC));
		Assert.assertTrue(str.endsWith("GMT"));
	}
}
