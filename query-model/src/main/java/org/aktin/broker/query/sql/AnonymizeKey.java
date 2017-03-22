package org.aktin.broker.query.sql;

import javax.xml.bind.annotation.XmlElement;

public class AnonymizeKey {
	@XmlElement
	TableColumn key;
	@XmlElement
	TableColumn[] ref;
}
