package org.aktin.broker.xml.util;

import java.time.Duration;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB adapter for processing java.time.Duration.
 * This can be removed later on when native support is added to the JAXB implementation
 *
 */
public class DurationAdapter extends XmlAdapter<String, Duration>{

	@Override
	public String marshal(Duration v) {
		if( v == null )return null;
		else return v.toString();
	}

	@Override
	public Duration unmarshal(String v) {
		if( v == null )return null;
		else return Duration.parse(v);
	}
	
}