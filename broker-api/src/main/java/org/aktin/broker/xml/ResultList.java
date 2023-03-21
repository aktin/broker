package org.aktin.broker.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="result-list")
public class ResultList {

	@XmlElement(name="result")
	List<ResultInfo> results;
	
	protected ResultList(){
	}
	public ResultList(List<ResultInfo> results){
		this.results = results;
	}
	public List<ResultInfo> getResults(){
		return results;
	}
	
}
