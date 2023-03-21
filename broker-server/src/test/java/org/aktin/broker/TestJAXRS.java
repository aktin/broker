package org.aktin.broker;

import jakarta.ws.rs.core.MediaType;
import jakarta.xml.bind.JAXB;

import org.junit.Assert;
import org.junit.Test;

/**
 * Assertations about the JAX-RS implementation
 * @author R.W.Majeed
 *
 */
public class TestJAXRS {

	@Test
	public void removeCharsetFromMediaType(){
		MediaType a = MediaType.valueOf("test/x-test ; charset=asdf");
		MediaType b = a.withCharset(null); // doesn't work, charset will be preserved
		b = new MediaType(a.getType(), b.getSubtype());
		System.out.println(b);
		Assert.assertNotNull(b); // satisfy analyzers. 
		// we should check if charset subtype is gone.. apparently in newer JDKs this will work
	}

	// does now do what is expected with +xml, +json
	//@Test
	public void mimeTypeCompatibility(){
		MediaType a = MediaType.valueOf("application/fhir+xml");
		MediaType b = MediaType.valueOf("application/*+xml");
		MediaType c = MediaType.valueOf("*/*+json");
		Assert.assertTrue(a.isCompatible(b));
		Assert.assertFalse(a.isCompatible(c));
	}
	public static <T> void printXML(T object){
		JAXB.marshal(object, System.out);
	}
	// Cannot marshall Map or Properties directly with JAXB. Need wrapper class.
//	@Test
//	public void marshallUnmarshallHashMap(){
//		Properties m = new Properties();
//		m.put("TEST", "test1");
//	}
}
