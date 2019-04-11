package org.aktin.broker.query.sql;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * JAXB object to declare a SQL table to be exported
 * @author R.W.Majeed
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ExportTable {
	public static final String DEFAULT_TYPE_TSV = "text/tab-separated-values";
	/**
	 * SQL table from which the data should be extracted
	 */
	@XmlAttribute(required=true)
	public String table;

	/**
	 * Destination media type for the exported data. 
	 * If undefined, {@code text/tab-separated-values}
	 * is used.
	 */
	@XmlAttribute(required=false)
	public String type;

	/**
	 * Destination name for the exported data file.
	 * If undefined, {@link #table} is used.
	 */
	@XmlAttribute(required=false)
	public String destination;
}
