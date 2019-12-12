package org.aktin.broker.query.aggregate.rscript;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name=RSource.XML_ELEMENT)
public class RSource {
	public static final String XML_ELEMENT = "r-script";
	public static final String XML_NAMESPACE = "http://aktin.org/ns/aggregate/r";
	@XmlElement
	Source source;
	@XmlElement
	List<Resource> resource;
	@XmlElement
	List<Result> result;

	@XmlElement(name="result-list")
	ResultList resultList;
}
