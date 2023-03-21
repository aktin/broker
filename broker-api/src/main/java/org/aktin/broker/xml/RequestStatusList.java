package org.aktin.broker.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="request-status-list")
public class RequestStatusList {

	@XmlElement(name="request-status-info")
	private List<RequestStatusInfo> list;

	protected RequestStatusList(){
	}
	public RequestStatusList(List<RequestStatusInfo> list){
		this.list = list;
	}
	public List<RequestStatusInfo> getStatusList(){
		return list;
	}
}
