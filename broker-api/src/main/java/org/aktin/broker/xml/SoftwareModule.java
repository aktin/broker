package org.aktin.broker.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import lombok.Getter;

@XmlAccessorType(XmlAccessType.FIELD)
public class SoftwareModule {
	/**
	 * Software module ID
	 */
	@XmlAttribute(required=true)
	@Getter
	String id;

	/**
	 * Software version associated with the {@link #id}
	 */
	@Getter
	@XmlElement(required=true)
	String version;

	/**
	 * Optional URL for the software module
	 */
	@Getter
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
