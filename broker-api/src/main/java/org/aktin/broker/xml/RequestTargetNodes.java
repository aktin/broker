package org.aktin.broker.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

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
