package org.aktin.broker.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="nodes")
@XmlAccessorType(XmlAccessType.NONE)
public class RequestTargetNodes {

	@XmlElement
	int[] node;
	
	protected RequestTargetNodes(){
	}
	public RequestTargetNodes(int[] nodes){
		this.node = nodes;
	}

	public int[] getNodes(){
		return node;
	}
}
