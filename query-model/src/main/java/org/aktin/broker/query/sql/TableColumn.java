package org.aktin.broker.query.sql;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class TableColumn {
	@XmlAttribute
	public String table;
	@XmlAttribute
	public String column;
}
