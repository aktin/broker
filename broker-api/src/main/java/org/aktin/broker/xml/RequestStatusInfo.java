package org.aktin.broker.xml;

import java.time.Instant;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Status information for a request as reported by a node.
 * <p>
 * All timestamps other than {@link #deleted} correspond to the
 * respective enum values of {@link RequestStatus}.
 * </p>
 * @author R.W.Majeed
 *
 */
@XmlRootElement(name="request-node-status")
@XmlAccessorType(XmlAccessType.FIELD)
public class RequestStatusInfo {
	public int node;
	public Instant deleted;

	// following timestamps correlate to the RequestState
	//
	// it seems to make sense to use an array and access the timestamps using
	// enum ordinals, but since this structure is used for JSON/XML serialisation,
	// the resulting document would loose readability significantly.
	public Instant retrieved;
	public Instant queued;
	public Instant processing;
	public Instant completed;
	public Instant rejected;
	public Instant failed;
	public Instant expired;
	/**
	 * The interaction timestamp is handled specially.
	 * When any other timestamp is updated, the interaction 
	 * timestamp should be cleared.
	 */
	public Instant interaction;
	/**
	 * Media type of the status message
	 */
	public String type;

	public RequestStatusInfo(){
	}
	public RequestStatusInfo(int node){
		this.node = node;
	}
	public RequestStatus getStatus(){
		if( failed != null ){
			return RequestStatus.failed;
		} else if(expired != null) {
			return RequestStatus.expired;
		}else if( rejected != null ){
			return RequestStatus.rejected;
		}else if( completed != null ){
			return RequestStatus.completed;
		}else if( interaction != null ){
			return RequestStatus.interaction;
		}else if( processing != null ){
			return RequestStatus.processing;
		}else if( queued != null ){
			return RequestStatus.queued;
		}else if( retrieved != null ){
			return RequestStatus.retrieved;
		}
		else return null;
	}
}
