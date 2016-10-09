package org.aktin.broker.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class SoftwareModule {
	@XmlAttribute(required=true)
	String id;
	@XmlElement(required=true)
	String version;
	@XmlElement(required=false)
	String url;

	public static final SoftwareModule BROKER_API = new SoftwareModule("broker-api", SoftwareModule.class.getPackage().getImplementationVersion());
	/** Constructor for JAXB */
	protected SoftwareModule(){
	}
	public SoftwareModule(String id, String version){
		this.version = version;
		this.id = id;
	}
}
