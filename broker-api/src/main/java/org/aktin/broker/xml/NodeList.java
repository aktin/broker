package org.aktin.broker.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="node-list")
public class NodeList {
	@XmlElement(name="node")
	List<Node> nodes;
	
	protected NodeList(){
	}
	
	public NodeList(List<Node> list){
		this.nodes = list;
	}
	public void addNode(Node node){
		nodes.add(node);
	}
	public List<Node> getNodes(){
		return nodes;
	}
}
