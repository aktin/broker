package org.aktin.broker;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXB;

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
}
