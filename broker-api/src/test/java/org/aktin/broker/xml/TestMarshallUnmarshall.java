package org.aktin.broker.xml;

import java.net.URL;

import javax.xml.bind.JAXB;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.aktin.broker.xml.NodeStatus;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class TestMarshallUnmarshall {
	@Test
	public void unmarshall_node_status(){
		NodeStatus n = JAXB.unmarshal(getResource("/node-status.xml"), NodeStatus.class);
		Assert.assertNotNull(n.timestamp);
		Assert.assertNotNull(n.uptime);
		System.out.println(n.timestamp);
		System.out.println(n.uptime);
		Assert.assertNull(n.payload);
	}
	@Test
	public void unmarshall_node_status_payload(){
		NodeStatus n = JAXB.unmarshal(getResource("/node-status-payload.xml"), NodeStatus.class);
		Assert.assertNotNull(n.timestamp);
		Assert.assertNotNull(n.uptime);
		System.out.println(n.timestamp);
		System.out.println(n.uptime);
		Assert.assertNotNull(n.payload);
		Assert.assertTrue(n.payload instanceof Element);
	}
	public Source getResource(URL url){
		SAXSource xml = new SAXSource(new InputSource(url.toString()));
		//xml.setSystemId(doc.toString());
		return xml;
	}
	public Source getResource(String name){
		return getResource(getClass().getResource(name));
	}
}
