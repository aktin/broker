package org.aktin.broker.query.sql;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class SQLQuery {
	@XmlElement
	List<Source> source;
	@XmlElement
	List<AnonymizeKey> anonymize;
	@XmlElement
	List<ExportTable> export;

}
