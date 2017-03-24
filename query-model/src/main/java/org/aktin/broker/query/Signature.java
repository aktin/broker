package org.aktin.broker.query;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * Signature during query exchange
 * 
 * @author R.W.Majeed
 *
 */
public class Signature {

	/** Signature author */
	@XmlAttribute
	String from;
	
	/** Signature algorithm */
	@XmlAttribute
	String algorithm;
	
	@XmlValue
	byte[] value;
	
	// TODO add code to verify content
}
