package org.aktin.broker.query.sql;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name=SQLQuery.XML_ELEMENT)
public class SQLQuery {
	public static final String XML_ELEMENT = "sql";
	public static final String XML_NAMESPACE = "http://aktin.org/ns/i2b2/sql";
	@XmlElement(name="temporary-table")
	List<TemporaryTable> tables;

	@XmlElement
	List<Source> source;
	@XmlElement
	List<AnonymizeKey> anonymize;
	@XmlElement
	List<ExportTable> export;

}
