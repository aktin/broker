package org.aktin.broker.query.sql;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class ExportTable {
	/**
	 * SQL table from which the data should be extracted
	 */
	@XmlAttribute(required=true)
	public String table;

	/**
	 * Destination name for the exported data file. If undefined, {@link #table} is used.
	 */
	@XmlAttribute(required=false)
	public String destination;
}
