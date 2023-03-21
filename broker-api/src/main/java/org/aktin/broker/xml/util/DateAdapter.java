package org.aktin.broker.xml.util;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;


public class DateAdapter extends XmlAdapter<String, Date>{

	@Override
	public Date unmarshal(String v) {
		if( v == null )return null;
		Calendar cal = DatatypeConverter.parseDateTime(v);
		return cal.getTime();
	}

	@Override
	public String marshal(Date v) {
		if( v == null )return null;
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		cal.setTime(v);
		return DatatypeConverter.printDateTime(cal);
	}
	
}