package org.aktin.broker;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="test-payload")
public class XmlPayload {

	public XmlPayload() {
	}
	public XmlPayload(String element, String attribute) {
		this.element = element;
		this.attribute = attribute;
	}
	
	@XmlElement
	String element;

	@XmlAttribute
	String attribute;
}
