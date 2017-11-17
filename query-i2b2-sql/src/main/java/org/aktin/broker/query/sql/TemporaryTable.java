package org.aktin.broker.query.sql;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * JAXB object to declare a temporary table
 * @author R.W.Majeed
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
public class TemporaryTable {
	/**
	 * SQL name for the temporary table
	 */
	@XmlAttribute
	String name;
}
