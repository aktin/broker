package org.aktin.broker.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="request-list")
public class RequestList {

	@XmlElement(name="request")
	List<RequestInfo> requests;

	protected RequestList(){
	}

	public RequestList(List<RequestInfo> requests){
		this.requests = requests;
	}
	
	public List<RequestInfo> getRequests(){
		return requests;
	}

	public static RequestList create(){
		RequestList rl = new RequestList();
		rl.requests = new ArrayList<>();
		return rl;
	}
	public boolean isEmpty(){
		return requests == null || requests.size() == 0;
	}
}
