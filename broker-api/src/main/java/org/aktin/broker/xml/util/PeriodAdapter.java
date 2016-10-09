package org.aktin.broker.xml.util;

import java.time.Period;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB adapter for processing java.time.Duration.
 * This can be removed later on when native support is added to the JAXB implementation
 *
 */
public class PeriodAdapter extends XmlAdapter<String, Period>{

	@Override
	public String marshal(Period v) {
		if( v == null )return null;
		else return v.toString();
	}

	@Override
	public Period unmarshal(String v) {
		if( v == null )return null;
		else return Period.parse(v);
	}
	
}