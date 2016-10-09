package org.aktin.broker.xml;

import java.time.Instant;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="node")
@XmlAccessorType(XmlAccessType.FIELD)
public class Node {
	public int id;
	public String clientDN;
	@XmlElement(name="last-contact")
	public Instant lastContact;
	
	protected Node(){
		
	}
	public Node(int id, String clientDN, Instant lastContact){
		this.id = id;
		this.clientDN = clientDN;
		this.lastContact = lastContact;
	}
}
