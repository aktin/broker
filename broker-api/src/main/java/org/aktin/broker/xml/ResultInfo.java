package org.aktin.broker.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class ResultInfo {

	/**
	 * Node id which submitted the result
	 */
	public String node;
	/**
	 * Media type for the result
	 */
	public String type;

	/** Constructor for JAXB **/
	protected ResultInfo(){ 

	}
	public ResultInfo(String node, String type){
		this.node = node;
		this.type = type;
	}
}
