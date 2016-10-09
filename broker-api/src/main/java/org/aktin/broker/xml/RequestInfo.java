package org.aktin.broker.xml;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="request")
public class RequestInfo {

	/** Unique id for the request */
	@XmlAttribute
	String id;

	public String getId(){
		return id;
	}
	/**
	 * Timestamp when the request was published. Future
	 * timestamps are not permitted.
	 */
	@XmlElement
	Instant published;

	/**
	 * Timestamp of global closing of request. Only admins
	 * can close requests.
	 * Future timestamps are permitted (e.g. to set a deadline)
	 */
	@XmlElement
	Instant closed;

	/**
	 * Media types of available query definitions
	 */
	@XmlElement(name="type")
	public String[] types;

	@XmlElement(name="node-status")
	public List<RequestStatusInfo> nodeStatus;
	
	protected RequestInfo(){
	}
	public RequestInfo(String id, Instant published, Instant closed){
		this.id = id;
		this.published = published;
		this.closed = closed;
	}
	public void setTypes(String[] types){
		this.types = types;
	}
	/**
	 * Determine whether the specified media type is present in the
	 * list of supported media types.
	 * @param type desired type
	 * @return {@code true} if the desired media type is supported. {@code false} otherwise.
	 */
	public boolean hasMediaType(String type){
		return Arrays.asList(types).contains(type);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((closed == null) ? 0 : closed.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((published == null) ? 0 : published.hashCode());
		result = prime * result + Arrays.hashCode(types);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RequestInfo other = (RequestInfo) obj;
		if( !Objects.equals(id, other.id) ){
			return false;
		}
		if( !Objects.equals(closed, other.closed) ){
			return false;
		}
		if( !Objects.equals(published, other.published) ){
			return false;
		}
		if (!Arrays.equals(types, other.types))
			return false;
		return true;
	}
	// TODO implement toString id, types
}
