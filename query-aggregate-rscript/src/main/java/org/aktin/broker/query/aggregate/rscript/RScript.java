package org.aktin.broker.query.aggregate.rscript;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name=RScript.XML_ELEMENT)
public class RScript {
	public static final String XML_ELEMENT = "r-script";
	public static final String XML_NAMESPACE = "http://aktin.org/ns/i2b2/sql";
	@XmlElement
	List<Source> source;
	@XmlElement
	List<ExportTable> export;

}
