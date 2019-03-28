package org.aktin.broker.query.aggregate.rscript;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)

public class Result {
	@XmlAttribute
	String type;
	@XmlAttribute
	String file;
}
