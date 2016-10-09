package org.aktin.broker.xml;

import java.time.Instant;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Status information for a request as reported by a node
 * 
 * @author R.W.Majeed
 *
 */
@XmlRootElement(name="request-node-status")
@XmlAccessorType(XmlAccessType.FIELD)
public class RequestStatusInfo {
	public String node;
	public Instant retrieved;
	public Instant deleted;
	public Instant accepted;
	public Instant processing;
	public Instant completed;
	public Instant rejected;
	public Instant failed;
	/**
	 * Media type of the status message
	 */
	public String type;

	public RequestStatusInfo(){
	}
	public RequestStatusInfo(String node){
		this.node = node;
	}
	public RequestStatus getStatus(){
		if( failed != null ){
			return RequestStatus.failed;
		}else if( rejected != null ){
			return RequestStatus.rejected;
		}else if( completed != null ){
			return RequestStatus.completed;
		}else if( processing != null ){
			return RequestStatus.processing;
		}else if( accepted != null ){
			return RequestStatus.accepted;
		}else if( retrieved != null ){
			return RequestStatus.retrieved;
		}
		else return null;
	}
}
