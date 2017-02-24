package org.aktin.broker.xml;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
 * Status for a data warehouse node, as reported by the node.
 * 
 * @author R.W.Majeed
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name="node-status")
public class NodeStatus {
	/**
	 * Current timestamp
	 */
	Date timestamp;
	/**
	 * Duration in epoch milliseconds, how long the node software application (e.g. J2EE deployment) is running (since startup)
	 */
	Long uptime;
	
	protected NodeStatus(){
	}
	
	public NodeStatus(Date startup){
		this.timestamp = new Date();
		this.uptime = System.currentTimeMillis() - timestamp.getTime();
	}
}
