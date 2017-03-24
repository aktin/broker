package org.aktin.broker.query.sql;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="sql")
public class SQLQuery {
	@XmlElement
	List<Source> source;
	@XmlElement
	List<AnonymizeKey> anonymize;
	@XmlElement
	List<ExportTable> export;

}
