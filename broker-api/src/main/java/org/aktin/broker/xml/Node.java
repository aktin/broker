package org.aktin.broker.xml;

import java.time.Instant;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Node information from server point of view.
 *
 * @author R.W.Majeed
 *
 */
@XmlRootElement(name="node")
@XmlAccessorType(XmlAccessType.FIELD)
public class Node {
	public int id;
	public String clientDN;
	@XmlElement(name="last-contact")
	public Instant lastContact;
	@XmlElement
	public boolean websocket;
	/**
	 * Relevant software modules running at the client. The first element must be "broker-api" with
	 * the current version information.
	 */
	@XmlElement
	public Map<String, String> modules;
	
	protected Node(){
		
	}
	public Node(int id, String clientDN, Instant lastContact){
		this.id = id;
		this.clientDN = clientDN;
		this.lastContact = lastContact;
	}

	/**
	 * Extract the common name component from the distinguished name.
	 * Current implementation expects the DN to start with CN.
	 * @return common name (CN) if available, otherwise {@code null}
	 */
	public String getCommonName(){
		// TODO follow standards. need to find CN also somewhere else in the string?
		if( clientDN != null && clientDN.startsWith("CN=") ){
			int e = clientDN.indexOf(',');
			if( e == -1 ){
				// no more components, use full string
				e = clientDN.length();
			}
			return clientDN.substring(3, e);
		}else{
			return null;
		}
	}
}
