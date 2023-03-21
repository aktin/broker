package org.aktin.broker.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;

@XmlAccessorType(XmlAccessType.FIELD)
public class ResultInfo {

	/**
	 * Node id which submitted the result
	 * XXX change to int?
	 */
	public int node;
	/**
	 * Media type for the result
	 */
	public String type;

	/** Constructor for JAXB **/
	protected ResultInfo(){ 

	}
	public ResultInfo(int node, String type){
		this.node = node;
		this.type = type;
	}
}
