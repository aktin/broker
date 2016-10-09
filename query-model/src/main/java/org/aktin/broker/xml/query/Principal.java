package org.aktin.broker.xml.query;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

/**
 * Principal of a query.
 * @author R.W.Majeed
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class Principal {

	/**
	 * Principal's full name
	 */
	@XmlElement(required=true)
	public String name;
	/**
	 * Principal's organisation
	 */
	@XmlElement(required=true)
	public String organisation;
	@XmlElement(required=true)
	public String email;
	@XmlElement(required=true)
	public String phone;
	/**
	 * Postal address
	 */
	public String address;
	/**
	 * Web page URL
	 */
	public String url;
}
