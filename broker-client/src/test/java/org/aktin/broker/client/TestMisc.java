package org.aktin.broker.client;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;

import org.aktin.broker.client2.BrokerClient2;
import org.aktin.broker.xml.RequestInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class TestMisc {

	@Test
	public void misc(){
		// output algorithm
		String defaultAlgo = KeyManagerFactory.getDefaultAlgorithm();
		Assertions.assertNotNull(defaultAlgo);
		System.out.println("KeyManagerFactory.defaultAlgo: "+defaultAlgo);
	}

	//enable for local testing unexpected http behaviour
	//@Test
	public void expectExceptionForNonXmlResponse() throws IOException {
		BrokerClient2 c = new BrokerClient2(URI.create("http://localhost:8080/bla"));

		List<RequestInfo>r = c.listMyRequests();

		System.out.println(r);

	}

}
